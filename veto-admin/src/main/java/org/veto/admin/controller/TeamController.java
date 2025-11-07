package org.veto.admin.controller;

import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.veto.core.rdbms.bean.Team;
import org.veto.core.rdbms.repository.TeamRepository;
import org.veto.shared.Util;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Collections;


@RequestMapping(value = "/team")
@Controller
public class TeamController {

    @Resource
    private TeamRepository teamRepository;

    @Value("${service.team.avatar.path}")
    private String teamAvatarPath;

    @GetMapping(value = "/get")
    public String getTeam(Model model, @RequestParam(value = "id", required = false) Integer id,
                          @RequestParam(value = "name", required = false) String name,
                          @RequestParam(value = "page",required = false, defaultValue = "1") Integer page
    ) {
        page = page < 1 ?  1 : page;

        Pageable pageable = PageRequest.of(page, 10);

        if (id != null) {
            model.addAttribute("data", teamRepository.findAllByIdIn(Collections.singletonList(id), pageable));
        }else if (Util.isNotBlank(name)) {
            model.addAttribute("data", teamRepository.findAllByCnNameContainingIgnoreCase(name, pageable));
        }else {
            model.addAttribute("data", teamRepository.findAll(pageable));
        }
        model.addAttribute("word", Util.isNotBlank(name) ? name : "");

        return "team-list";
    }

    @GetMapping(value = "/edit/{id}")
    public String editTeam(Model model, @PathVariable Integer id) {
        Team team = teamRepository.findById(id).orElse(null);
        model.addAttribute("data", team);
        return "team-edit";
    }

    @PostMapping(value = "/update")
    public String updateTeam(Model model, @RequestParam("id") Integer id, @RequestParam("name") String name, @RequestParam("icon")MultipartFile icon) {
        if (name != null){
            if (name.length() > 120){
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "名称长度过长");
                return "error";
            }
            if (teamRepository.existsByCnNameAndIdNot(name, id)) {
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "球队名称已存在");
                return "error";
            }
        }
        if (icon != null){
            if (IndexController.getContentType(icon.getName()).equalsIgnoreCase("application/octet-stream")) {
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "不支持的球队logo图片");
                return "error";
            }else if (icon.getSize() > 1024 * 1024 * 5){
                model.addAttribute("errorTitle", "参数错误");
                model.addAttribute("errorMessage", "球队图片必须在5MB以下");
                return "error";
            }
        }

        Team team = teamRepository.findById(id).orElse(null);
        if (team == null){
            model.addAttribute("errorTitle", "参数错误");
            model.addAttribute("errorMessage", "球队信息存在");
            return "error";
        }

        if (Util.isNotBlank(name)){
            team.setCnName(name);
        }
        if (icon != null){
            try {
                icon.transferTo(Paths.get(this.teamAvatarPath, team.getIcon()));
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        return "redirect:/team/get";
    }
}
