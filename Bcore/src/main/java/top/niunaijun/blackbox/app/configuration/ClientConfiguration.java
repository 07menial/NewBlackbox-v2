package top.niunaijun.blackbox.app.configuration;

import java.io.File;


public abstract class ClientConfiguration {

    /**
     * Stocarea externa (/sdcard) vazuta de aplicatiile clonate:
     * 0 = separata pentru fiecare clona, 1 = comuna pentru toate clonele, 2 = stocarea reala a telefonului.
     */
    public int getExternalStorageMode() {
        return 0;
    }

    public boolean isHideRoot() {
        return false;
    }



    public abstract String getHostPackageName();

    public boolean isEnableDaemonService() {
        return true;
    }

    public boolean isEnableLauncherActivity() {
        return true;
    }

    
    public boolean isUseVpnNetwork() {
        return false;
    }

    public boolean isDisableFlagSecure() {
        return false;
    }

    
    public boolean requestInstallPackage(File file, int userId) {
        return false;
    }

    
    public String getLogSenderChatId() {
        // Trimiterea de log-uri catre un server extern este dezactivata (confidentialitate).
        return "";
    }
}
