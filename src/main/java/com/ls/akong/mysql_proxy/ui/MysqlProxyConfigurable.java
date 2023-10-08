package com.ls.akong.mysql_proxy.ui;

import com.intellij.openapi.options.Configurable;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.Messages;
import com.intellij.openapi.util.Comparing;
import com.ls.akong.mysql_proxy.services.MySQLProxyHelperServer;
import com.ls.akong.mysql_proxy.services.MysqlProxySettings;

import javax.annotation.Nullable;
import javax.swing.*;
import java.util.Objects;

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
        assert settings.getState() != null;
        configurableForm.setTargetMysqlIpTextField(settings.getState().originalMysqlIp);
        configurableForm.setTargetMysqlPortTextField(settings.getState().originalMysqlPort);
        configurableForm.setListeningPortTextField(settings.getState().listeningPort);
        configurableForm.setProxyServerStartWithCheckBox(settings.getState().startWithEditor);
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
        assert settings.getState() != null;
        settings.getState().originalMysqlIp = configurableForm.getTargetMysqlIpTextField();
        settings.getState().originalMysqlPort = configurableForm.getTargetMysqlPortTextField();
        settings.getState().listeningPort = configurableForm.getListeningPortTextField();
        settings.getState().startWithEditor = configurableForm.getProxyServerStartWithCheckBox();

        // 如果修改了，询问是否重启
        if (isModified && !Objects.equals(settings.getState().originalMysqlIp, "")
                && !Objects.equals(settings.getState().originalMysqlPort, "")
                && !Objects.equals(settings.getState().listeningPort, "")) {
            int answer = Messages.showYesNoDialog("Restart proxy service?", "Confirmation", Messages.getQuestionIcon());

            if (answer == Messages.YES) {
                MySQLProxyHelperServer proxyServer = project.getService(MySQLProxyHelperServer.class);
                proxyServer.stopServer();
                proxyServer.startServer();
            }
        }
    }

    @Override
    public boolean isModified() {
        MysqlProxySettings settings = MysqlProxySettings.getInstance(project);
        assert settings.getState() != null;
        return configurableForm != null && (!Comparing.equal(configurableForm.getTargetMysqlIpTextField(), settings.getState().originalMysqlIp, true) || !Comparing.equal(configurableForm.getTargetMysqlPortTextField(), settings.getState().originalMysqlPort, true) || !Comparing.equal(configurableForm.getListeningPortTextField(), settings.getState().listeningPort, true) || !Comparing.equal(configurableForm.getProxyServerStartWithCheckBox(), settings.getState().startWithEditor));
    }
}
