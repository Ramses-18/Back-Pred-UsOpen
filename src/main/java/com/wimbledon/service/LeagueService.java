package com.wimbledon.service;

import com.wimbledon.dto.*;
import com.wimbledon.entity.*;
import com.wimbledon.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class LeagueService {

    private final LeagueRepository leagueRepo;
    private final LeagueMemberRepository memberRepo;
    private final UserRepository userRepo;
    private final ScoreService scoreService;

    private static final SecureRandom RNG = new SecureRandom();

    @Transactional
    public LeagueDto createLeague(String ownerEmail, CreateLeagueRequest req) {
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre de la liga no puede estar vacío.");
        }

        User owner = userRepo.findByEmail(ownerEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        String code;
        do {
            code = generateCode();
        } while (leagueRepo.existsByCode(code));

        League league = leagueRepo.save(League.builder()
            .owner(owner)
            .name(req.getName().trim())
            .code(code)
            .build());

        // El dueño se une automáticamente
        memberRepo.save(LeagueMember.builder()
            .league(league)
            .user(owner)
            .build());

        log.info("[createLeague] liga '{}' creada con código {} por {}", league.getName(), code, ownerEmail);
        return toDto(league, owner.getId());
    }

    @Transactional
    public LeagueDto joinLeague(String userEmail, JoinLeagueRequest req) {
        League league = leagueRepo.findByCode(req.getCode())
            .orElseThrow(() -> new IllegalArgumentException("Código de liga inválido."));

        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        if (memberRepo.existsByLeagueIdAndUserId(league.getId(), user.getId())) {
            throw new IllegalStateException("Ya sos miembro de esta liga.");
        }

        memberRepo.save(LeagueMember.builder()
            .league(league)
            .user(user)
            .build());

        log.info("[joinLeague] {} se unió a liga '{}' ({})", userEmail, league.getName(), league.getCode());
        return toDto(league, user.getId());
    }

    @Transactional
    public void leaveLeague(String userEmail, Long leagueId) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        League league = leagueRepo.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException("Liga no encontrada."));

        if (league.getOwner().getId().equals(user.getId())) {
            throw new IllegalStateException("El dueño no puede abandonar la liga. Eliminala o transfiere la propiedad.");
        }

        LeagueMember member = memberRepo.findByLeagueIdAndUserId(leagueId, user.getId())
            .orElseThrow(() -> new IllegalArgumentException("No sos miembro de esta liga."));

        memberRepo.delete(member);
        log.info("[leaveLeague] {} abandonó la liga '{}'", userEmail, league.getName());
    }

    public List<LeagueDto> getMyLeagues(String userEmail) {
        User user = userRepo.findByEmail(userEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        // Ligas donde soy miembro
        List<LeagueMember> memberships = memberRepo.findByUserId(user.getId());
        return memberships.stream()
            .map(m -> toDto(m.getLeague(), user.getId()))
            .collect(Collectors.toList());
    }

    public LeagueLeaderboardDto getLeagueLeaderboard(Long leagueId) {
        League league = leagueRepo.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException("Liga no encontrada."));

        List<Long> userIds = memberRepo.findUserIdsByLeagueId(leagueId);
        List<LeaderboardEntryDto> fullLeaderboard = scoreService.buildLeaderboard();

        List<LeaderboardEntryDto> members = fullLeaderboard.stream()
            .filter(e -> userIds.contains(e.getUserId()))
            .collect(Collectors.toList());

        // Re-rankear dentro de la liga
        for (int i = 0; i < members.size(); i++) {
            members.get(i).setRank(i + 1);
        }

        return LeagueLeaderboardDto.builder()
            .leagueId(league.getId())
            .leagueName(league.getName())
            .leagueCode(league.getCode())
            .members(members)
            .build();
    }

    @Transactional
    public void deleteLeague(String ownerEmail, Long leagueId) {
        User owner = userRepo.findByEmail(ownerEmail)
            .orElseThrow(() -> new IllegalArgumentException("Usuario no encontrado."));

        League league = leagueRepo.findById(leagueId)
            .orElseThrow(() -> new IllegalArgumentException("Liga no encontrada."));

        if (!league.getOwner().getId().equals(owner.getId())) {
            throw new IllegalStateException("Solo el dueño puede eliminar la liga.");
        }

        // Borrar miembros primero
        List<LeagueMember> members = memberRepo.findByLeagueId(leagueId);
        memberRepo.deleteAll(members);
        leagueRepo.delete(league);

        log.info("[deleteLeague] liga '{}' eliminada por {}", league.getName(), ownerEmail);
    }

    private LeagueDto toDto(League league, Long currentUserId) {
        int count = (int) memberRepo.countByLeagueId(league.getId());
        return LeagueDto.builder()
            .id(league.getId())
            .name(league.getName())
            .code(league.getCode())
            .ownerName(league.getOwner().getName())
            .memberCount(count)
            .isOwner(league.getOwner().getId().equals(currentUserId))
            .isMember(memberRepo.existsByLeagueIdAndUserId(league.getId(), currentUserId))
            .build();
    }

    private String generateCode() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
        StringBuilder sb = new StringBuilder(6);
        for (int i = 0; i < 6; i++) {
            sb.append(chars.charAt(RNG.nextInt(chars.length())));
        }
        return sb.toString();
    }
}