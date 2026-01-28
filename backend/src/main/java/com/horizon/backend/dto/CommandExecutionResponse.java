package com.horizon.backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CommandExecutionResponse {
    private String output;
    private String error;
    private int exitCode;
    
    public CommandExecutionResponse(String output, int exitCode) {
        this.output = output;
        this.exitCode = exitCode;
        this.error = "";
    }
}
