package com.wimbledon.service;

import com.wimbledon.dto.TournamentDto;
import com.wimbledon.entity.Tournament;
import com.wimbledon.repository.TournamentRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TournamentManagementService {

    private final TournamentRepository tournamentRepo;

    public List<TournamentDto> getAllTournaments() {
        return tournamentRepo.findAll().stream()
            .map(this::toDto)
            .collect(Collectors.toList());
    }

    public TournamentDto getActiveTournament() {
        return tournamentRepo.findByActiveTrue()
            .map(this::toDto)
            .orElse(null);
    }

    @Transactional
    public TournamentDto createTournament(String name, String slug) {
        if (tournamentRepo.findBySlug(slug).isPresent()) {
            throw new IllegalArgumentException("Ya existe un torneo con ese slug.");
        }

        // Desactivar torneo anterior
        tournamentRepo.findByActiveTrue().ifPresent(t -> {
            t.setActive(false);
            tournamentRepo.save(t);
        });

        Tournament t = tournamentRepo.save(Tournament.builder()
            .name(name)
            .slug(slug)
            .active(true)
            .build());

        log.info("[createTournament] torneo '{}' ({}) creado", name, slug);
        return toDto(t);
    }

    private TournamentDto toDto(Tournament t) {
        return TournamentDto.builder()
            .id(t.getId())
            .name(t.getName())
            .slug(t.getSlug())
            .active(Boolean.TRUE.equals(t.getActive()))
            .build();
    }
}