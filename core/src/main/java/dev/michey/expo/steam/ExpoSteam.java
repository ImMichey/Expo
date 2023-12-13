package dev.michey.expo.steam;

import com.codedisaster.steamworks.*;
import dev.michey.expo.log.ExpoLogger;
import dev.michey.expo.util.ClientPackets;
import dev.michey.expo.util.ClientStatic;

import java.nio.ByteBuffer;
import java.nio.charset.Charset;

public class ExpoSteam {

    public static SteamHTTPCallback httpCallback = new SteamHTTPCallback() {

        @Override
        public void onHTTPRequestCompleted(SteamHTTPRequestHandle request, long contextValue, boolean requestSuccessful, SteamHTTP.HTTPStatusCode statusCode, int bodySize) {
            ExpoLogger.log("onHTTPRequestCompleted(" + request.toString() + " " + contextValue + " " + requestSuccessful + " " + statusCode.toString() + " " + bodySize + ")");

            ByteBuffer bodyData = ByteBuffer.allocateDirect(bodySize);

            try {

                if (http.getHTTPResponseBodyData(request, bodyData)) {

                    byte[] dest = new byte[bodyData.limit()];
                    bodyData.get(dest);

                    String result = new String(dest, Charset.defaultCharset());
                    System.out.println("=== begin result:\n" + result + "\n=== end result");

                } else {
                    System.out.println("- failed reading request data!");
                }

            } catch (SteamException e) {
                e.printStackTrace();
            }

            http.releaseHTTPRequest(request);
        }

        @Override
        public void onHTTPRequestHeadersReceived(SteamHTTPRequestHandle request, long contextValue) {
            ExpoLogger.log("onHTTPRequestHeadersReceived(" + request.toString() + " " + contextValue + ")");
        }

        @Override
        public void onHTTPRequestDataReceived(SteamHTTPRequestHandle request, long contextValue, int offset, int bytesReceived) {
            ExpoLogger.log("onHTTPRequestDataReceived(" + request.toString() + " " + contextValue+ " " + offset + " " + bytesReceived + ")");
        }

    };

    private static final SteamHTTP http = new SteamHTTP(httpCallback);

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

        @Override
        public void onGetTicketForWebApi(SteamAuthTicket authTicket, SteamResult result, byte[] ticketData) {
            ExpoLogger.log("onGetTicketForWebApi( " + authTicket.toString() + " " + result.toString() + " " + ticketData.length + ")");

            ClientPackets.p45AuthReq(ClientStatic.PLAYER_USERNAME, ticketData);

            /*
            SteamHTTPRequestHandle request = http.createHTTPRequest(SteamHTTP.HTTPMethod.GET,
                    "https://api.steampowered.com/ISteamUserAuth/AuthenticateUserTicket/v1/?format=json");

            http.setHTTPRequestGetOrPostParameter(request, "key", "A6493809DD5329BF81C04A2750E8DB18");
            http.setHTTPRequestGetOrPostParameter(request, "appid", String.valueOf(ExpoShared.STEAM_APP_ID));
            http.setHTTPRequestGetOrPostParameter(request, "ticket", bytesToHex(ticketData));

            SteamAPICall call = http.sendHTTPRequest(request);

            if(!call.isValid()) {
                ExpoLogger.log("Failed to send");
            }
            */
        }

    };

    public static SteamFriendsCallback friendsCallback = new SteamFriendsCallback() {

        @Override
        public void onSetPersonaNameResponse(boolean success, boolean localSuccess, SteamResult result) {
            ExpoLogger.log("onSetPersonaNameResponse(" + success + " " + localSuccess + " " + result.toString() + ")");
        }

        @Override
        public void onPersonaStateChange(SteamID steamID, SteamFriends.PersonaChange change) {
            ExpoLogger.log("onPersonaStateChange(" + steamID.getAccountID() + " " + change.toString() + ")");
        }

        @Override
        public void onGameOverlayActivated(boolean active, boolean userInitiated, int appID) {
            ExpoLogger.log("onGameOverlayActivated(" + active + " " + userInitiated + " " + appID + ")");
        }

        @Override
        public void onGameLobbyJoinRequested(SteamID steamIDLobby, SteamID steamIDFriend) {
            ExpoLogger.log("onGameLobbyJoinRequested(" + steamIDLobby.getAccountID() + " " + steamIDFriend.getAccountID() + ")");
        }

        @Override
        public void onAvatarImageLoaded(SteamID steamID, int image, int width, int height) {
            ExpoLogger.log("onAvatarImageLoaded(" + steamID.getAccountID() + " " + image + " " + width + " " + width + ")");
        }

        @Override
        public void onFriendRichPresenceUpdate(SteamID steamIDFriend, int appID) {
            ExpoLogger.log("onFriendRichPresenceUpdate(" + steamIDFriend.getAccountID() + " " + appID + ")");
        }

        @Override
        public void onGameRichPresenceJoinRequested(SteamID steamIDFriend, String connect) {
            ExpoLogger.log("onGameRichPresenceJoinRequested(" + steamIDFriend.getAccountID() + " " + connect + ")");
        }

        @Override
        public void onGameServerChangeRequested(String server, String password) {
            ExpoLogger.log("onGameServerChangeRequested(" + server + " " + password + ")");
        }

    };

}
