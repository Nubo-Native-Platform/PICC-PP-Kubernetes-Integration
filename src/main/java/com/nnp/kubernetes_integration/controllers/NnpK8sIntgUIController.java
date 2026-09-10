package com.nnp.kubernetes_integration.controllers;

import com.nnp.kubernetes_integration.dtos.K8sTerminalRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@Tag(name = "Terminal UI Controller", description = "Web endpoints for interactive pod terminal UI rendering")
public class NnpK8sIntgUIController {

    @Value("${VAR.BASE.NNP.DOMAIN:localhost:8080}")
    private String host;

    @Operation(summary = "Render interactive web terminal UI (GET)", description = "Renders the xterm.js terminal web interface connecting to the pod shell over WebSocket")
    @GetMapping("/terminal")
    public String getTerminalPage(
            @Parameter(description = "Target NNP environment name", required = true) @RequestParam String envName,
            @Parameter(description = "Target Pod name", required = true) @RequestParam String pod,
            @Parameter(description = "Target Container name within Pod (optional)") @RequestParam(required = false) String container,
            Model model
    ) {

        model.addAttribute("envName", envName);
        model.addAttribute("pod", pod);
        model.addAttribute("container", container != null ? container : "");
        model.addAttribute("host", host);

        return "terminal";
    }

    @Operation(summary = "Render interactive web terminal UI (POST)", description = "Renders the xterm.js terminal web interface using request payload")
    @PostMapping("/terminal")
    public String getTerminalPage(@RequestBody @Valid K8sTerminalRequest terminalRequest, Model model) {

        model.addAttribute("envName", terminalRequest.envName());
        model.addAttribute("pod", terminalRequest.pod());
        model.addAttribute("container", StringUtils.hasText(terminalRequest.container()) ? terminalRequest.container() : "");
        model.addAttribute("host", host);

        return "terminal";
    }

}
