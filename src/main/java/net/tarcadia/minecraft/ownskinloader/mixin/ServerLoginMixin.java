package net.tarcadia.minecraft.ownskinloader.mixin;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.mojang.authlib.GameProfile;
import com.mojang.authlib.properties.Property;
import com.mojang.authlib.properties.PropertyMap;
import net.minecraft.server.network.ServerLoginNetworkHandler;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import javax.net.ssl.HttpsURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

@Mixin(ServerLoginNetworkHandler.class)
public class ServerLoginMixin {

    private static final String URL_API_NAME_TO_UUID = "https://api.mojang.com/users/profiles/minecraft/";
    private static final String URL_API_UUID_TO_PROFILE = "https://sessionserver.mojang.com/session/minecraft/profile/";
    private PropertyMap texturePropertyMap = null;

    @Shadow @Final private static AtomicInteger NEXT_AUTHENTICATOR_THREAD_ID;
    @Shadow @Final static Logger LOGGER;
    @Shadow @Nullable GameProfile profile;

    @Inject(method = "acceptPlayer", at = @At("HEAD"), cancellable = true)
    private void acceptPlayer(CallbackInfo ci) {
        if (this.texturePropertyMap == null) {
            ci.cancel();
        } else if (this.profile != null) {
            this.profile.getProperties().putAll(texturePropertyMap);
        }
    }

    @Inject(method = "onHello", at = @At("TAIL"))
    private void onHello(CallbackInfo ci) {
        Thread thread = new Thread("User Authenticator #" + NEXT_AUTHENTICATOR_THREAD_ID.incrementAndGet() + " (MIXIN)"){
            @Override
            public void run() {
                String playerName;
                if ((ServerLoginMixin.this.profile != null) && ((playerName = ServerLoginMixin.this.profile.getName()) != null)) {
                    String uuid = null;
                    JsonObject tx;
                    try {
                        var url = new URL(URL_API_NAME_TO_UUID + playerName);
                        LOGGER.info("Requesting: \"" + url + "\" for player UUID.");
                        var https = (HttpsURLConnection) url.openConnection();
                        https.setRequestMethod("GET");
                        https.setRequestProperty("Content-Type", "application/json");
                        https.connect();
                        var code = https.getResponseCode();
                        if (code >= 200 && code < 300) {
                            var message = new String(https.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                            uuid = JsonParser.parseString(message).getAsJsonObject().get("id").getAsString();
                        } else {
                            LOGGER.warn("Unable to fetch player " + playerName + "'s UUID.");
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Unable to fetch player " + playerName + "'s UUID.");
                    }

                    if (uuid != null) try {
                        var url = new URL(URL_API_UUID_TO_PROFILE + uuid + "?unsigned=false");
                        LOGGER.info("Requesting: \"" + url + "\" for player profile.");
                        var https = (HttpsURLConnection) url.openConnection();
                        https.setRequestMethod("GET");
                        https.setRequestProperty("Content-Type", "application/json");
                        https.connect();
                        var code = https.getResponseCode();
                        if (code >= 200 && code < 300) {
                            var message = new String(https.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
                            tx = JsonParser.parseString(message).getAsJsonObject().getAsJsonArray("properties").get(0).getAsJsonObject();
                            Property texture = new Property("textures", tx.get("value").getAsString(), tx.get("signature").getAsString());
                            PropertyMap pm = new PropertyMap();
                            pm.put("textures", texture);
                            ServerLoginMixin.this.texturePropertyMap = pm;
                            return;
                        } else {
                            LOGGER.warn("Unable to fetch player " + playerName + "'s profile.");
                        }
                    } catch (Exception e) {
                        LOGGER.warn("Unable to fetch player " + playerName + "'s profile.");
                    }
                }
                ServerLoginMixin.this.texturePropertyMap = new PropertyMap();
            }
        };
        thread.start();
    }

    @Inject(method = "toOfflineProfile", at = @At("RETURN"), cancellable = true)
    private void toOfflineProfile(GameProfile profile, CallbackInfoReturnable<GameProfile> info) {
        GameProfile ret = info.getReturnValue();
        ret.getProperties().putAll(profile.getProperties());
        //LOGGER.info("GameProfile:" + profile);
        //LOGGER.info("GameProfile Ret:" + ret);
        info.setReturnValue(ret);
    }

}
