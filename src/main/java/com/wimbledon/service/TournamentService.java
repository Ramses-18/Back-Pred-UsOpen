package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TournamentService {

    private final TournamentPickRepository   tPickRepo;
    private final TournamentResultRepository tResultRepo;
    private final UserRepository             userRepo;

    public TournamentPickDto getMyPick(String email) {
        User user = findUser(email);
        return tPickRepo.findByUserId(user.getId())
            .map(tp -> TournamentPickDto.builder()
                .champion(tp.getChampion())
                .semis(List.of(e(tp.getSemi1()), e(tp.getSemi2()), e(tp.getSemi3()), e(tp.getSemi4())))
                .build())
            .orElse(TournamentPickDto.builder().semis(List.of("","","","")).build());
    }

    @Transactional
    public TournamentPickDto savePick(TournamentPickRequest req, String email) {
        User user = findUser(email);
        TournamentPick tp = tPickRepo.findByUserId(user.getId())
            .orElse(TournamentPick.builder().user(user).build());
        tp.setChampion(req.getChampion());
        List<String> s = req.getSemis() != null ? req.getSemis() : List.of();
        tp.setSemi1(s.size() > 0 ? s.get(0) : null);
        tp.setSemi2(s.size() > 1 ? s.get(1) : null);
        tp.setSemi3(s.size() > 2 ? s.get(2) : null);
        tp.setSemi4(s.size() > 3 ? s.get(3) : null);
        tp.setUpdatedAt(LocalDateTime.now());
        tPickRepo.save(tp);
        return getMyPick(email);
    }

    public TournamentPickDto getResult() {
        return tResultRepo.findTopByOrderByIdDesc()
            .map(tr -> TournamentPickDto.builder()
                .champion(tr.getChampion())
                .semis(List.of(e(tr.getSemi1()), e(tr.getSemi2()), e(tr.getSemi3()), e(tr.getSemi4())))
                .build())
            .orElse(TournamentPickDto.builder().semis(List.of("","","","")).build());
    }

    @Transactional
    public TournamentPickDto saveResult(TournamentPickRequest req) {
        TournamentResult tr = tResultRepo.findTopByOrderByIdDesc()
            .orElse(TournamentResult.builder().build());
        tr.setChampion(req.getChampion());
        List<String> s = req.getSemis() != null ? req.getSemis() : List.of();
        tr.setSemi1(s.size() > 0 ? s.get(0) : null);
        tr.setSemi2(s.size() > 1 ? s.get(1) : null);
        tr.setSemi3(s.size() > 2 ? s.get(2) : null);
        tr.setSemi4(s.size() > 3 ? s.get(3) : null);
        tr.setUpdatedAt(LocalDateTime.now());
        tResultRepo.save(tr);
        return getResult();
    }

    private User findUser(String email) { return userRepo.findByEmail(email).orElseThrow(); }
    private String e(String s) { return s == null ? "" : s; }
}
