package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.diagnostic.Logger;
import com.intellij.openapi.project.Project;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;
import com.ls.akong.mysql_proxy.services.NotificationsService;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.CompletableFuture;

/**
 * 检查当前商城的最新版本，如果大于当前版本，则弹框提示用户
 */
public class VersionUpdateChecker {
    private static final Logger logger = Logger.getInstance(VersionUpdateChecker.class);

    // 当前版本号，除了这里还有：gradle.properties 的 version
    private static final String version = "1.0.10";
    private static final String apiVersionUrl = "https://plugins.jetbrains.com/api/plugins/22655/updates";
    private static String latestVersion = "";

    /**
     * 获取当前商城的最新版本
     *
     * @return
     */
    public static String getLatestVersion() {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpGet httpGet = new HttpGet(apiVersionUrl);
            String response = EntityUtils.toString(httpClient.execute(httpGet).getEntity());

            // 解析 JSON 数据
            JSONArray updates = new JSONArray(response);
            if (updates.length() > 0) {
                JSONObject latestUpdate = updates.getJSONObject(0);
                return latestUpdate.getString("version");
            }
        } catch (Exception e) {
            // 处理网络请求异常
            e.printStackTrace();
        }

        return null;
    }

    private static boolean isVersionSkipped(Project project, String version) {
        return MysqlProxySettings.getInstance(project).isVersionSkipped(version);
    }

    /**
     * 版本通知提醒
     */
    public static void versionUpdateNotification(Project project) {
        CompletableFuture.runAsync(() -> {
            if (isUpdateAvailable(project, version)) {
                NotificationsService.notifyUpdateAvailable(project, latestVersion);
            }
        });
    }

    /**
     * 检查是否有新版本
     *
     * @param currentVersion
     * @return
     */
    public static boolean isUpdateAvailable(Project project, String currentVersion) {
        // 获取最新的版本号
        latestVersion = getLatestVersion();
        logger.info("latest version is " + latestVersion);


        // 检查是是否已标记跳过该版本
        if (isVersionSkipped(project, latestVersion)) {
            return false;
        }

        if (currentVersion == null || latestVersion == null) {
            // 处理版本号为空的情况
            return false;
        }

        return !currentVersion.equals(latestVersion);
    }

}
