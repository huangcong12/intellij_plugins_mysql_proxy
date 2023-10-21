package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.ls.akong.mysql_proxy.services.MySQLProxyHelperServer;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;
import com.ls.akong.mysql_proxy.services.PersistingSensitiveDataService;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;

import static java.lang.Thread.sleep;

public class MysqlProxyConfigurable implements Configurable {

    private final Project project;  // Project object to store the passed project
    private ConfigurableForm configurableForm;

    // Constructor to receive the Project object
    public MysqlProxyConfigurable(Project project) {
        this.project = project;
    }

    @Override
    public String getDisplayName() {
        return "Mysql Proxy Plugin Configuration";
    }

    @Nullable
    @Override
    public JComponent createComponent() {
        if (configurableForm == null) {
            configurableForm = new ConfigurableForm(project);
        }

        MysqlProxySettings settings = MysqlProxySettings.getInstance(project);
        configurableForm.setTargetMysqlIpTextField(settings.getOriginalMysqlIp());
        configurableForm.setTargetMysqlPortTextField(settings.getOriginalMysqlPort());
        configurableForm.setDatabase(settings.getDatabase());
        configurableForm.setUsername(settings.getUsername());
        configurableForm.setPassword(PersistingSensitiveDataService.getPassword());
        configurableForm.setListeningPortTextField(settings.getListeningPort());
        configurableForm.setProxyServerStartWithCheckBox(settings.isStartWithEditor());
        return configurableForm.getPanel();
    }

    @Override
    public void apply() {
        // port 最大是 65535
        if (!Objects.equals(configurableForm.getListeningPortTextField(), "") && Integer.parseInt(configurableForm.getListeningPortTextField()) > 65535) {
            Messages.showErrorDialog("Invalid Proxy Listener Port.The port number must be between 1 and 65535. Please provide a valid port number.", "Invalid Port Number");
            return;
        }
        // 在保持前就要判断好
        boolean isModified = isModified();

        MysqlProxySettings settings = MysqlProxySettings.getInstance(project);
        settings.setOriginalMysqlIp(configurableForm.getTargetMysqlIpTextField());
        settings.setOriginalMysqlPort(configurableForm.getTargetMysqlPortTextField());
        settings.setDatabase(configurableForm.getDatabase());
        settings.setUsername(configurableForm.getUsername());
        settings.setListeningPort(configurableForm.getListeningPortTextField());
        settings.setStartWithEditor(configurableForm.getProxyServerStartWithCheckBox());
        PersistingSensitiveDataService.storePassword(configurableForm.getPassword());

        // 如果修改了，询问是否重启
        if (isModified && !Objects.equals(settings.getOriginalMysqlIp(), "")
                && !Objects.equals(settings.getOriginalMysqlPort(), "")
                && !Objects.equals(settings.getListeningPort(), "")) {
            int answer = Messages.showYesNoDialog("Restart proxy service?", "Confirmation", Messages.getQuestionIcon());

            if (answer == Messages.YES) {
                MySQLProxyHelperServer proxyServer = project.getService(MySQLProxyHelperServer.class);
                proxyServer.stopServer();
                try {
                    sleep(1000);
                } catch (InterruptedException e) {
                    throw new RuntimeException(e);
                }
                proxyServer.startServer();
            }
        }
    }

    @Override
    public boolean isModified() {
        MysqlProxySettings settings = MysqlProxySettings.getInstance(project);
        return configurableForm != null && (
                !Comparing.equal(configurableForm.getTargetMysqlIpTextField(), settings.getOriginalMysqlIp(), true)
                        || !Comparing.equal(configurableForm.getTargetMysqlPortTextField(), settings.getOriginalMysqlPort(), true)
                        || !Comparing.equal(configurableForm.getDatabase(), settings.getDatabase(), true)
                        || !Comparing.equal(configurableForm.getUsername(), settings.getUsername(), true)
                        || !Comparing.equal(configurableForm.getPassword(), PersistingSensitiveDataService.getPassword(), true)
                        || !Comparing.equal(configurableForm.getListeningPortTextField(), settings.getListeningPort(), true)
                        || !Comparing.equal(configurableForm.getProxyServerStartWithCheckBox(), settings.isStartWithEditor()));
    }
}
