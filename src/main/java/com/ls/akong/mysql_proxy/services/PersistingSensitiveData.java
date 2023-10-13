package com.ls.akong.mysql_proxy.services;

import com.intellij.credentialStore.CredentialAttributes;
import com.intellij.credentialStore.CredentialAttributesKt;
import com.intellij.ide.passwordSafe.PasswordSafe;
import com.intellij.openapi.components.Service;

@Service(Service.Level.PROJECT)
public class PersistingSensitiveData {

    private final static String databaseSensitiveKey = "DATABASE_SENSITIVE_KEY";

    private static CredentialAttributes createCredentialAttributes(String key) {
        return new CredentialAttributes(
                CredentialAttributesKt.generateServiceName("MySQLProxySystem", key)
        );
    }

    /**
     * 获取密码
     *
     * @return
     */
    public static String getPassword() {
        CredentialAttributes credentialAttributes = createCredentialAttributes(databaseSensitiveKey);
        return PasswordSafe.getInstance().getPassword(credentialAttributes);
    }

    /**
     * 保持 username and password
     *
     * @param password
     */
    public static void storePassword(String password) {
        CredentialAttributes credentialAttributes = createCredentialAttributes(databaseSensitiveKey); // see previous sample
        PasswordSafe.getInstance().setPassword(credentialAttributes, password);
    }
}
