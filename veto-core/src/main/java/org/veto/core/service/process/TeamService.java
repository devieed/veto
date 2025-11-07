package org.veto.core.service.process;
import java.util.Date;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.veto.core.rdbms.bean.Team;
import org.veto.core.rdbms.repository.TeamRepository;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
public class TeamService {

    @Value("${service.team.avatar.path}")
    private String teamAvatarPath;

    @Resource
    private TeamRepository teamRepository;

    private ObjectMapper objectMapper = new ObjectMapper();

    private static final String URL = "https://api.footballant.com/api/v3/fifa-club-list?page=%s&pageSize=300&updateTime=2025-7&lang=zh-CN&device=1";

    public void processByPlaywright(){

    }

    /**
     * 更新球队基本信息
     * @throws IOException
     */
    public void process() throws IOException {

        int page = 0;

        while (true) {
            page++;
            String data = Jsoup.connect(URL.formatted(page)).ignoreContentType(true).timeout(60000).execute().body();

            JsonNode jsonNode = objectMapper.readTree(data);

            JsonNode list = jsonNode.get("data").get("list");

            if (list.isArray() && !list.isEmpty()) {
                for (JsonNode teamNode : list) {
                    String enName = teamNode.get("teamNameEn").asText();
                    String cnName = teamNode.get("teamNameCn").asText();
                    String teamImage = teamNode.get("teamImg").asText();
                    String rank = teamNode.get("rank").asText();
                    String score = teamNode.get("score").asText();

                    if(teamRepository.existsByCnName(cnName)){
                        log.warn("duplicate name {}", cnName);
                        continue;
                    }

                    Team team = new Team();
                    team.setCnName(cnName);
                    team.setEnName(enName);
                    team.setIcon("");
                    team.setCreatedAt(new Date());
                    team.setCountry("");
                    team.setStatus(true);

                    team = teamRepository.save(team);

                    Map<String, String> headers = new HashMap<>();

                    headers.put("accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8,application/signed-exchange;v=b3;q=0.7");
                    headers.put("accept-encoding", "gzip, deflate, br");
                    headers.put("accept-language", "zh-CN");
                    headers.put("cache-control", "no-cache");
                    headers.put("cookie", "_gcl_au=1.1.1963075732.1757864566; _ga_NRJD2XHXDB=GS2.1.s1757864566$o1$g0$t1757864566$j60$l0$h0; _ga=GA1.1.1892101217.1757864566; _ga_L009K2X827=GS2.1.s1757864566$o1$g0$t1757864566$j60$l0$h0; cf_clearance=beKDRl9Oy3ET0nkqR6CMjT4E6.3Bj5aMJThK5WEWeY0-1758685954-1.2.1.1-o2qcrmm5wu2Co25PVuYqpUmenqBeKD2UbtDzh1MWAog0jHtLzJ2QmGc_Q6S2JbMUEOh3RHnBR_nVReAtEDFo.qg5TnOwUrRCeDpRbitB0ez6oZzQiRA64sni3EboJi_CeJAPj2xvVrjZIkOYUQdF8tFb9XEJJnB9DE8UQadr7Pm1ptcq6jvNj0oJ0OTuEYt5igz.1MWWou6CVObWnJTQYlNg0b0h2Axq6K03ykFVgQc");
                    headers.put("dnt", "1");
                    headers.put("pragma", "no-cache");
                    headers.put("priority", "u=0, i");
                    headers.put("referer", "https://api.footballant.com/");
                    headers.put("sec-ch-ua", """
                            "Chromium";v="140", "Not=A?Brand";v="24", "Microsoft Edge";v="140\"""");
                    headers.put("sec-ch-ua-mobile", "?0");
                    headers.put("user-agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/140.0.0.0 Safari/537.36 Edg/140.0.0.0");


                    var response = Jsoup.connect(teamImage).headers(headers).ignoreContentType(true).execute();

                    System.out.println(response.statusCode());

                    String iconName = team.getId() + ".png";

                    try(FileOutputStream fos = new FileOutputStream(teamAvatarPath + "/" + iconName)){
                        var steam = response.bodyStream();
                        steam.transferTo(fos);
                        fos.flush();
                    }
                    team.setIcon(iconName);

                    teamRepository.save(team);

                    log.warn("save team {}", team.toString());
                }
            }else {
                break;
            }
        }
    }
}
