package dev.michey.expo.steam;

import com.codedisaster.steamworks.*;
import dev.michey.expo.log.ExpoLogger;

public class ExpoSteam {

    public static SteamUserCallback callback = new SteamUserCallback() {

        @Override
        public void onAuthSessionTicket(SteamAuthTicket steamAuthTicket, SteamResult steamResult) {
            ExpoLogger.log("onAuthSessionTicket(" + steamAuthTicket.toString() + " " + steamResult.toString() + ")");
        }

        @Override
        public void onValidateAuthTicket(SteamID steamID, SteamAuth.AuthSessionResponse authSessionResponse, SteamID steamID1) {
            ExpoLogger.log("onValidateAuthTicket(" + steamID.toString() + " " + authSessionResponse.toString() + " " + steamID1.toString() + ")");
        }

        @Override
        public void onMicroTxnAuthorization(int i, long l, boolean b) {
            ExpoLogger.log("onMicroTxnAuthorization(" + i + " " + l + " " + b + ")");
        }

        @Override
        public void onEncryptedAppTicket(SteamResult steamResult) {
            ExpoLogger.log("onEncryptedAppTicket(" + steamResult.toString() + ")");
        }

    };

}
