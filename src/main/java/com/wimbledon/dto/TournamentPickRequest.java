package com.wimbledon.dto;
import lombok.Data;
import java.util.List;
@Data
public class TournamentPickRequest {
    private String champion;// Nombre del jugador que el usuario predice que ganará el torneo
    private List<String> semis;
}
