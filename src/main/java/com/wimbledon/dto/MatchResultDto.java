package com.wimbledon.dto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MatchResultDto {

   // public static Object builder() {
     //   throw new UnsupportedOperationException("Not supported yet."); // Generated from nbfs://nbhost/SystemFileSystem/Templates/Classes/Code/GeneratedMethodBody
    //}
    
    private String winner;
    private Integer setsWinner;
    private Integer gamesWinner;
    private Integer gamesLoser;
}
