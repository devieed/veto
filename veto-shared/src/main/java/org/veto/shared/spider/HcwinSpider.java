package org.veto.shared.spider;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.microsoft.playwright.*;
import com.microsoft.playwright.options.ColorScheme;
import com.microsoft.playwright.options.Proxy;
import com.microsoft.playwright.options.WaitUntilState;
import lombok.extern.slf4j.Slf4j;
import org.jsoup.Jsoup;
import org.veto.shared.ODD_SCORE;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
public class HcwinSpider implements Runnable {

    // 配置常量
    private static final String DOMAIN = "https://hcwin866.com/auth/login";
    private static final Pattern TEAM_SITE_ID_MATCH_BY_URL = Pattern.compile("^https://team-cdn\\.leisulive\\.com/(\\d+)\\.png$");
    private static final int MAX_PAGE_NUM = 10;

    private String username;

    private String password;

    private String proxy;

    // Playwright 实例
    private Playwright playwright;
    private Browser browser;
    private BrowserContext context;
    private Page page;

    // 配置参数
    private final boolean headless;
    private final double timeout;
    private final double autoStopLimitTime;

    // 数据收集
    private final List<CapturedContest> contests = new CopyOnWriteArrayList<>();
    private final Set<String> teamsDuplicate = new ConcurrentSkipListSet<>();
    private final Map<String, CapturedImage> teamImages = new ConcurrentHashMap<>();

    // 同步控制
    private final CountDownLatch finishedLatch = new CountDownLatch(2);
    private final CountDownLatch completionLatch = new CountDownLatch(1);
    private final AtomicInteger finishedPageCount = new AtomicInteger(0);
    private final AtomicInteger noStartPageCount = new AtomicInteger(0);

    // 构造函数
    public HcwinSpider() {
        this(false, 30000, TimeUnit.MINUTES.toMillis(10));
    }

    public HcwinSpider(boolean headless) {
        this(headless, 30000, TimeUnit.MINUTES.toMillis(10));
    }

    public HcwinSpider(boolean headless, double timeout) {
        this(headless, timeout, TimeUnit.MINUTES.toMillis(10));
    }

    public HcwinSpider(boolean headless, double timeout, double autoStopLimitTime, String username, String password){
        this.headless = headless;
        this.timeout = timeout;
        this.autoStopLimitTime = autoStopLimitTime;
        this.username = username;
        this.password = password;
    }

    public void setProxy(String proxy) {
        this.proxy = proxy;
    }

    public HcwinSpider(boolean headless, double timeout, double autoStopLimitTime) {
        this(headless, timeout, autoStopLimitTime, "15010244273", "asdf2237");
    }

    // Getters
    public List<CapturedContest> getContests() {
        return new ArrayList<>(contests);
    }

    public CountDownLatch getAwaitCountSuccess() {
        return completionLatch;
    }

    @Override
    public void run() {
        log.info("Starting HcwinSpider in {} mode", headless ? "headless" : "headed");

        try {
            // 1. 初始化 Playwright
            initializePlaywright();

            // 2. 设置事件监听器
            setupEventListeners();

            // 3. 导航到登录页面并登录
            navigateAndLogin();

            // 4. 点击全部比赛
            clickAllContests();

            // 5. 等待数据收集完成
            waitForDataCollection();

            log.info("Spider execution completed successfully. Collected {} contests", contests.size());

        } catch (Throwable e) {
            log.error("Spider execution failed", e);
        } finally {
            completionLatch.countDown();
            shutdown();
        }
    }

    /**
     * 初始化 Playwright 浏览器和上下文
     */
    private void initializePlaywright() {
        log.info("Initializing Playwright...");

        playwright = Playwright.create();

        // 配置浏览器启动选项
        BrowserType.LaunchOptions launchOptions = new BrowserType.LaunchOptions()
                .setHeadless(headless)
                .setSlowMo(100); // 增加延迟，使操作更像真人
        if (this.proxy != null){
            launchOptions = launchOptions.setProxy(new Proxy(this.proxy));
        }

        // 通用浏览器参数（headless 和非 headless 都使用）
        List<String> args = new ArrayList<>(Arrays.asList(
                "--disable-blink-features=AutomationControlled",
                "--disable-dev-shm-usage",
                "--no-sandbox"
        ));

        if (headless) {
            // headless 模式额外参数
            args.addAll(Arrays.asList(
                    "--disable-gpu",
                    "--disable-software-rasterizer",
                    "--disable-setuid-sandbox",
                    "--disable-background-networking",
                    "--disable-background-timer-throttling",
                    "--disable-backgrounding-occluded-windows",
                    "--disable-breakpad",
                    "--disable-component-extensions-with-background-pages",
                    "--disable-extensions",
                    "--disable-features=TranslateUI,BlinkGenPropertyTrees",
                    "--disable-ipc-flooding-protection",
                    "--disable-renderer-backgrounding",
                    "--enable-features=NetworkService,NetworkServiceInProcess",
                    "--force-color-profile=srgb",
                    "--hide-scrollbars",
                    "--metrics-recording-only",
                    "--mute-audio",
                    "--window-size=1920,1080"
            ));
        }

        launchOptions.setArgs(args);

        browser = playwright.chromium().launch(launchOptions);

        // 配置浏览器上下文（模拟真实浏览器）
        Browser.NewContextOptions contextOptions = new Browser.NewContextOptions()
                .setUserAgent("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
                .setViewportSize(1920, 1080)
                .setLocale("zh-CN")
                .setTimezoneId("Asia/Shanghai")
                .setColorScheme(ColorScheme.LIGHT)
                .setExtraHTTPHeaders(Map.of(
                        "Accept-Language", "zh-CN,zh;q=0.9,en;q=0.8",
                        "Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8",
                        "sec-ch-ua", "\"Not_A Brand\";v=\"8\", \"Chromium\";v=\"120\", \"Google Chrome\";v=\"120\"",
                        "sec-ch-ua-mobile", "?0",
                        "sec-ch-ua-platform", "\"Windows\""
                ));

        context = browser.newContext(contextOptions);
        
        // 设置默认超时
        context.setDefaultTimeout(timeout);
        
        page = context.newPage();

        // 注入更完善的反检测脚本
        page.addInitScript(
                "() => {" +
                        "  // 隐藏 webdriver" +
                        "  Object.defineProperty(navigator, 'webdriver', {get: () => undefined});" +
                        "  // 添加 chrome 对象" +
                        "  window.navigator.chrome = { runtime: {}, loadTimes: function() {}, csi: function() {}, app: {} };" +
                        "  // 模拟插件" +
                        "  Object.defineProperty(navigator, 'plugins', {get: () => [1, 2, 3, 4, 5]});" +
                        "  // 语言设置" +
                        "  Object.defineProperty(navigator, 'languages', {get: () => ['zh-CN', 'zh', 'en']});" +
                        "  // 权限查询" +
                        "  const originalQuery = window.navigator.permissions.query;" +
                        "  window.navigator.permissions.query = (parameters) => (" +
                        "    parameters.name === 'notifications' ? Promise.resolve({ state: Notification.permission }) : originalQuery(parameters)" +
                        "  );" +
                        "  // 添加 connection 属性" +
                        "  Object.defineProperty(navigator, 'connection', {" +
                        "    get: () => ({ effectiveType: '4g', rtt: 100, downlink: 10, saveData: false })" +
                        "  });" +
                        "}"
        );

        log.info("Playwright initialized successfully with headless={}", headless);
    }

    /**
     * 设置响应事件监听器
     */
    private void setupEventListeners() {
        page.onResponse(response -> {
            try {
                String url = response.url();
                String method = response.request().method();
                log.debug("Response captured - URL: {}, Method: {}", url, method);
                
                // 已结束的比赛
                if (url.contains("/api/match/list") && method.equalsIgnoreCase("POST")) {
                    String postData = response.request().postData();
                    log.info("Captured /api/match/list request, postData: {}", postData);
                    if (postData != null) {
                        JsonObject json = JsonParser.parseString(postData).getAsJsonObject();
                        if (json.has("type") && json.get("type").getAsString().equalsIgnoreCase("finished")) {
                            log.info("Processing finished contests...");
                            handleFinishedContest(response);
                        }
                    }
                }
                // 未开始的比赛
                else if (url.contains("/api/match/search-details-by-time") && method.equalsIgnoreCase("POST")) {
                    log.info("Processing no-start contests...");
                    handleNoStartContest(response);
                }
                // 队伍图标（如需要）
                else if (url.matches("^https://team-cdn\\.leisulive\\.com/\\d+\\.png$")) {
                    // handleTeamIcon(response.url(), response.body());
                }

            } catch (Exception e) {
                log.error("Error handling response: {}", response.url(), e);
            }
        });
    }

    /**
     * 导航到登录页面并执行登录
     */
    private void navigateAndLogin() throws InterruptedException {
        log.info("Navigating to login page...");

        // 等待验证码接口响应
        Response captchaResponse = page.waitForResponse(
                res -> res.url().contains("/api/get-code-captcha") &&
                        res.request().method().equalsIgnoreCase("POST"),
                () -> page.navigate(DOMAIN, new Page.NavigateOptions()
                        .setWaitUntil(WaitUntilState.DOMCONTENTLOADED)
                        .setTimeout(timeout))
        );

        log.info("Login page loaded, performing login...");
        login(captchaResponse);

        // 等待登录成功（等待首页元素出现）
        page.waitForSelector("div.home_nav_bg__v6KJO", new Page.WaitForSelectorOptions().setTimeout(timeout));
        log.info("Login successful");
    }

    /**
     * 执行登录操作
     */
    private void login(Response captchaResponse) {
        try {
            // 等待验证码图片加载
            page.waitForSelector("#captcha_img", new Page.WaitForSelectorOptions().setTimeout(timeout));
            log.info("Captcha image loaded");
            
            // 稍作等待，确保页面完全加载
            page.waitForTimeout(1000);

            // 解析验证码
            String captcha = parseCaptcha(captchaResponse);
            log.info("Captcha parsed: {}", captcha);

            // 填写用户名
            page.waitForSelector("#username", new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.fill("#username", this.username);
            log.info("Username filled");
            
            // 等待并填写密码
            page.waitForSelector("#password", new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.waitForTimeout(500);
            page.fill("#password", this.password);
            log.info("Password filled");
            
            // 填写验证码
            page.waitForSelector("#captchaElem", new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.fill("#captchaElem", captcha);
            log.info("Captcha filled");

            // 等待并点击同意条款
            String agreementSelector = "#__next > div > div:nth-child(1) > div > div > div.login_form_box__1ltx9 > div.login_f_content__2JL1J > div > div:nth-child(5) > div > span";
            page.waitForSelector(agreementSelector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.waitForTimeout(500);
            page.click(agreementSelector);
            log.info("Agreement checkbox clicked");

            // 等待并点击弹出框确认按钮
            String dialogButtonSelector = ".next-dialog-footer.next-align-center button";
            page.waitForSelector(dialogButtonSelector, new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.waitForTimeout(500);
            page.click(dialogButtonSelector);
            log.info("Dialog confirm button clicked");

            // 等待登录按钮可用
            page.waitForSelector(".login_sub_btn__2D-07", new Page.WaitForSelectorOptions().setTimeout(timeout));
            page.waitForTimeout(500);

            // 点击登录
            page.click(".next-btn-helper");
            log.info("Login button clicked");

        } catch (Exception e) {
            log.error("Login failed", e);
            throw new RuntimeException("Login failed", e);
        }
    }

    /**
     * 解析验证码
     */
    private String parseCaptcha(Response response) {
        try {
            String responseText = response.text();
            JsonObject json = JsonParser.parseString(responseText).getAsJsonObject();
            String base64Svg = json.getAsJsonObject("data").get("image").getAsString();

            // 移除 base64 前缀
            if (base64Svg.contains(",")) {
                base64Svg = base64Svg.substring(base64Svg.indexOf(',') + 1);
            }

            // 解码并解析 SVG
            byte[] decodedBytes = Base64.getDecoder().decode(base64Svg);
            String svgContent = new String(decodedBytes, StandardCharsets.UTF_8);

            return Jsoup.parse(svgContent).select("text").text().replaceAll("\\s+", "");

        } catch (Exception e) {
            log.error("Failed to parse captcha", e);
            throw new RuntimeException("Failed to parse captcha", e);
        }
    }

    /**
     * 点击"全部比赛"按钮
     */
    private void clickAllContests() {
        log.info("Clicking 'All Contests' button...");

        String contestButtonSelector = "div.home_nav_bg__v6KJO > div.home_item_btn_n__2SiwS:nth-of-type(3)";
        page.waitForSelector(contestButtonSelector, new Page.WaitForSelectorOptions().setTimeout(timeout));
        page.waitForTimeout(1000);
        
        page.click(contestButtonSelector);
        log.info("'All Contests' button clicked");
        
        // 等待弹窗出现
        try {
            page.waitForSelector(".next-overlay-wrapper.opened", new Page.WaitForSelectorOptions().setTimeout(timeout));
            log.info("Contest dialog opened");
            
            // 额外等待，确保弹窗内容加载完成
            page.waitForTimeout(2000);
            
            // 等待弹窗内的数据容器出现
            page.waitForSelector(".match_match_datas_list__3osIH", new Page.WaitForSelectorOptions().setTimeout(timeout));
            log.info("Contest data container loaded");
            
        } catch (Exception e) {
            log.warn("Failed to wait for contest dialog, continuing anyway", e);
        }
    }

    /**
     * 等待数据收集完成
     */
    private void waitForDataCollection() throws InterruptedException {
        log.info("Waiting for data collection to complete...");
        
        // 启动一个监控线程，定期检查状态
        ScheduledExecutorService monitor = Executors.newSingleThreadScheduledExecutor();
        monitor.scheduleAtFixedRate(() -> {
            log.info("Current status - Finished pages: {}, NoStart pages: {}, Total contests: {}", 
                    finishedPageCount.get(), noStartPageCount.get(), contests.size());
        }, 5, 5, TimeUnit.SECONDS);

        boolean completed = finishedLatch.await((long) autoStopLimitTime, TimeUnit.MILLISECONDS);
        
        monitor.shutdown();

        if (completed) {
            log.info("Data collection completed successfully. Finished pages: {}, NoStart pages: {}, Total contests: {}",
                    finishedPageCount.get(), noStartPageCount.get(), contests.size());
        } else {
            log.warn("Data collection timeout after {} ms. Finished pages: {}, NoStart pages: {}, Total contests collected: {}", 
                    autoStopLimitTime, finishedPageCount.get(), noStartPageCount.get(), contests.size());
            
            // 如果一个都没收集到，可能是监听器没生效
            if (contests.isEmpty()) {
                log.error("No contests collected! This might indicate that API request listeners are not working.");
                log.error("Please check if the API endpoints have changed or if the page structure is different.");
            }
        }
    }

    /**
     * 处理已结束的比赛数据
     */
    private void handleFinishedContest(Response response) {
        try {
            JsonObject responseResult = JsonParser.parseString(response.text()).getAsJsonObject();

            if (!responseResult.get("status").getAsBoolean()) {
                log.warn("Failed to get finished contest list: {}", responseResult);
                return;
            }

            JsonObject result = responseResult.getAsJsonObject("data").getAsJsonObject("search_result");
            int currentPage = result.get("current_page").getAsInt();
            int lastPage = result.get("last_page").getAsInt();

            log.info("Processing finished contests - Page {}/{}", currentPage, lastPage);

            // 解析比赛数据
            for (JsonElement contestJson : result.getAsJsonArray("data")) {
                CapturedContest contest = parseFinishedContest(contestJson.getAsJsonObject());
                if (contest != null) {
                    contests.add(contest);
                }
            }

            finishedPageCount.set(currentPage);

            // 判断是否需要继续滚动加载
            if (currentPage >= MAX_PAGE_NUM || currentPage >= lastPage) {
                log.info("Finished contest collection completed");
                finishedLatch.countDown();
            } else {
                scrollToLoadMore("body > div.next-overlay-wrapper.opened > div.next-dialog.next-overlay-inner > div > div > div.resultData_show_data_box__w_90C > div > div:nth-child(1) > div:nth-child(2) > div:nth-child(2) > div > div:nth-child(1) > div");
            }

        } catch (Exception e) {
            log.error("Error processing finished contest", e);
        }
    }

    /**
     * 解析已结束的比赛数据
     */
    private CapturedContest parseFinishedContest(JsonObject contestJson) {
        try {
            CapturedContest contest = new CapturedContest();

            // 比分
            contest.setHomeTeamFullScore(getIntOrNull(contestJson, "fthomescore"));
            contest.setAwayTeamFullScore(getIntOrNull(contestJson, "fhawayscore"));
            contest.setHomeHalfScore(getIntOrNull(contestJson, "fhhomescore"));
            contest.setAwayTeamHalfScore(getIntOrNull(contestJson, "fhawayscore"));

            // 主队信息
            JsonObject homeTeamJson = contestJson.getAsJsonObject("matches_home_team");
            CapturedTeam homeTeam = new CapturedTeam();
            homeTeam.setCnName(homeTeamJson.get("home_name_cn").getAsString());
            homeTeam.setEnName(homeTeamJson.get("home_name_en").getAsString());
            homeTeam.setIconUrl("https://team-cdn.leisulive.com/" + homeTeamJson.get("home_image").getAsString());
            contest.setHomeTeam(homeTeam);

            teamsDuplicate.add(homeTeam.getCnName());

            // 客队信息
            JsonObject awayTeamJson = contestJson.getAsJsonObject("matches_away_team");
            CapturedTeam awayTeam = new CapturedTeam();
            awayTeam.setCnName(awayTeamJson.get("away_name_cn").getAsString());
            awayTeam.setEnName(awayTeamJson.get("away_name_en").getAsString());
            awayTeam.setIconUrl("https://team-cdn.leisulive.com/" + awayTeamJson.get("away_image").getAsString());
            contest.setAwayTeam(awayTeam);

            teamsDuplicate.add(awayTeam.getCnName());

            // 联赛信息
            JsonObject leagueJson = contestJson.getAsJsonObject("matches_league");
            contest.setCnName(leagueJson.get("league_name_cn").getAsString());
            contest.setEnName(leagueJson.get("league_name_en").getAsString());

            // 开始时间
            String startTime = contestJson.get("kickoffdate").getAsString();
            contest.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTime));

            // 错误信息
            String errMsg = contestJson.get("match_error_text").getAsString();
            contest.setExist(errMsg != null && !errMsg.isBlank());
            if (contest.getExist()) {
                log.warn("Contest has error: {}", errMsg);
            }

            contest.setIsFinished(true);

            return contest;

        } catch (Exception e) {
            log.error("Error parsing finished contest", e);
            return null;
        }
    }

    /**
     * 处理未开始的比赛数据
     */
    private void handleNoStartContest(Response response) {
        try {
            JsonObject responseResult = JsonParser.parseString(response.text()).getAsJsonObject();

            if (!responseResult.get("status").getAsBoolean()) {
                log.warn("Failed to get no-start contest list: {}", responseResult);
                return;
            }

            JsonObject data = responseResult.getAsJsonObject("data");
            int currentPage = data.get("page").getAsInt();
            int lastPage = data.get("lastpage").getAsInt();

            log.info("Processing no-start contests - Page {}/{}", currentPage, lastPage);

            // 解析比赛数据
            for (JsonElement contestData : data.getAsJsonArray("search_result")) {
                CapturedContest contest = parseNoStartContest(contestData.getAsJsonObject());
                if (contest != null) {
                    contests.add(contest);
                }
            }

            noStartPageCount.set(currentPage);

            // 判断是否需要继续滚动加载
            if (currentPage >= MAX_PAGE_NUM || currentPage >= lastPage) {
                log.info("No-start contest collection completed");
                finishedLatch.countDown();
            } else {
                scrollToLoadMore("#__next > div > div:nth-child(1) > div > div > div.match_con_box__2c0kf > div.match_match_datas_list__3osIH > div > div:nth-child(1) > div");
            }

        } catch (Exception e) {
            log.error("Error processing no-start contest", e);
        }
    }

    /**
     * 解析未开始的比赛数据
     */
    private CapturedContest parseNoStartContest(JsonObject contestJson) {
        try {
            CapturedContest contest = new CapturedContest();

            // 联赛信息
            JsonObject leagueJson = contestJson.getAsJsonObject("matches_league");
            contest.setCnName(leagueJson.get("league_name_cn").getAsString());
            contest.setEnName(leagueJson.get("league_name_en").getAsString());

            // 主队信息
            JsonObject homeTeamJson = contestJson.getAsJsonObject("matches_home_team");
            CapturedTeam homeTeam = new CapturedTeam();
            homeTeam.setCnName(homeTeamJson.get("home_name_cn").getAsString());
            homeTeam.setEnName(homeTeamJson.get("home_name_en").getAsString());
            homeTeam.setIconUrl("https://team-cdn.leisulive.com/" + homeTeamJson.get("home_image").getAsString());
            contest.setHomeTeam(homeTeam);

            teamsDuplicate.add(homeTeam.getCnName());

            // 客队信息
            JsonObject awayTeamJson = contestJson.getAsJsonObject("matches_away_team");
            CapturedTeam awayTeam = new CapturedTeam();
            awayTeam.setCnName(awayTeamJson.get("away_name_cn").getAsString());
            awayTeam.setEnName(awayTeamJson.get("away_name_en").getAsString());
            awayTeam.setIconUrl("https://team-cdn.leisulive.com/" + awayTeamJson.get("away_image").getAsString());
            contest.setAwayTeam(awayTeam);

            teamsDuplicate.add(awayTeam.getCnName());

            // 开始时间
            String startTime = contestJson.get("kickoffdate").getAsString();
            contest.setStartTime(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").parse(startTime));

            // 赔率数据
            JsonObject odds = contestJson.getAsJsonObject("match_detail").getAsJsonObject("order_table");
            List<CapturedOddsScore> scores = new ArrayList<>();

            // 解析半场赔率
            JsonObject halfOdds = odds.getAsJsonObject("fh");
            scores.addAll(parseOdds(halfOdds, true));

            // 解析全场赔率
            JsonObject fullOdds = odds.getAsJsonObject("ft");
            scores.addAll(parseOdds(fullOdds, false));

            contest.setCapturedOddsScores(scores);
            contest.setExist(false);
            contest.setIsFinished(false);

            return contest;

        } catch (Exception e) {
            log.error("Error parsing no-start contest", e);
            return null;
        }
    }

    /**
     * 解析赔率数据
     */
    private List<CapturedOddsScore> parseOdds(JsonObject oddsJson, boolean isHalf) {
        List<CapturedOddsScore> scores = new ArrayList<>();

        for (String key : oddsJson.keySet()) {
            try {
                String scoreStr = key.replaceAll("\\D", "");
                if (scoreStr.length() != 2) {
                    log.warn("Invalid score format: {}", key);
                    continue;
                }

                // 转换为 "X-Y" 格式
                String score = key.contains("home")
                        ? scoreStr.charAt(0) + "-" + scoreStr.charAt(1)
                        : scoreStr.charAt(1) + "-" + scoreStr.charAt(0);

                JsonObject oddsData = oddsJson.getAsJsonObject(key);
                boolean isActive = oddsData.get("is_active").getAsBoolean();
                String oddRate = isActive ? oddsData.get("opp_rate").getAsString() : "0";

                CapturedOddsScore capturedOddsScore = new CapturedOddsScore();
                capturedOddsScore.setIsHalf(isHalf);
                capturedOddsScore.setScore(ODD_SCORE.me(score));
                capturedOddsScore.setOddRate(new BigDecimal(oddRate));
                capturedOddsScore.setIsActive(isActive);

                scores.add(capturedOddsScore);

            } catch (Exception e) {
                log.error("Error parsing odds for key: {}", key, e);
            }
        }

        return scores;
    }

    /**
     * 滚动加载更多数据
     */
    private void scrollToLoadMore(String selector) {
        try {
            ElementHandle element = page.querySelector(selector);
            if (element != null) {
                element.evaluate("el => el.scrollTop = el.scrollHeight");
                // 给页面一点时间加载
                page.waitForTimeout(500);
            }
        } catch (Exception e) {
            log.error("Error scrolling to load more", e);
        }
    }

    /**
     * 获取可能为 null 的整数值
     */
    private Integer getIntOrNull(JsonObject json, String key) {
        return json.get(key).isJsonNull() ? null : json.get(key).getAsInt();
    }

    /**
     * 关闭浏览器和 Playwright
     */
    public void shutdown() {
        log.info("Shutting down spider...");

        try {
            if (page != null && !page.isClosed()) {
                page.close();
                log.debug("Page closed");
            }
        } catch (Exception e) {
            log.error("Error closing page", e);
        }

        try {
            if (context != null) {
                context.close();
                log.debug("Context closed");
            }
        } catch (Exception e) {
            log.error("Error closing context", e);
        }

        try {
            if (browser != null && browser.isConnected()) {
                browser.close();
                log.debug("Browser closed");
            }
        } catch (Exception e) {
            log.error("Error closing browser", e);
        }

        try {
            if (playwright != null) {
                playwright.close();
                log.debug("Playwright closed");
            }
        } catch (Exception e) {
            log.error("Error closing playwright", e);
        }

        log.info("Spider shutdown completed");
    }

    public static void main(String[] args) {
        HcwinSpider spider = new HcwinSpider(false, 30000, TimeUnit.MINUTES.toMillis(10), "jjlin921114", "asdf2237");
        spider.run();

        log.info("Collected {} contests", spider.getContests().size());
        for (CapturedContest contest : spider.getContests()) {
            log.info("Contest: {} vs {}",
                    contest.getHomeTeam().getCnName(),
                    contest.getAwayTeam().getCnName());
        }
    }
}