package com.ls.akong.mysql_proxy.util;

import com.intellij.openapi.project.Project;
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
    private static final String version = "1.0.8";
    private static final String apiVersionUrl = "https://plugins.jetbrains.com/api/plugins/22655/updates";

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

    /**
     * 版本通知提醒
     */
    public static void versionUpdateNotification(Project project) {
        CompletableFuture.runAsync(() -> {
            if (isUpdateAvailable(version)) {
                NotificationsService.notifyUpdateAvailable(project);
            }
        });
    }

    /**
     * 检查是否有新版本
     *
     * @param currentVersion
     * @return
     */
    public static boolean isUpdateAvailable(String currentVersion) {
        // 获取最新的版本号
        String latestVersion = getLatestVersion();

        if (currentVersion == null || latestVersion == null) {
            // 处理版本号为空的情况
            return false;
        }

        String[] currentVersionParts = currentVersion.split("\\.");
        String[] latestVersionParts = latestVersion.split("\\.");

        for (int i = 0; i < Math.min(currentVersionParts.length, latestVersionParts.length); i++) {
            int currentPart = Integer.parseInt(currentVersionParts[i]);
            int latestPart = Integer.parseInt(latestVersionParts[i]);

            if (currentPart < latestPart) {
                return true;
            } else if (currentPart > latestPart) {
                return false;
            }
        }

        return latestVersionParts.length > currentVersionParts.length;
    }

}
