package com.bilicraft.networkdiagnosishelper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;

import java.util.List;

@AllArgsConstructor
@Data
@Builder
public class CommandContainer {
    private String type;
    private boolean traceroute;
    private boolean ping;
    private boolean checkReachable;
    private boolean dnsLookup;
    private boolean netCard;
    @Singular
    private List<String> hosts;
}
