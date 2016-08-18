package ink.abb.pogo.api.util;

import POGOProtos.Networking.Envelopes.AuthTicketOuterClass;
import POGOProtos.Networking.Envelopes.RequestEnvelopeOuterClass;
import POGOProtos.Networking.Envelopes.SignatureOuterClass;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass;
import POGOProtos.Networking.Envelopes.Unknown6OuterClass.Unknown6.Unknown2;
import POGOProtos.Networking.Requests.RequestOuterClass;
import com.google.protobuf.ByteString;
import ink.abb.pogo.api.PoGoApi;
import net.jpountz.xxhash.StreamingXXHash32;
import net.jpountz.xxhash.StreamingXXHash64;
import net.jpountz.xxhash.XXHashFactory;

import java.util.Random;

public class Signature {

    /**
     * Given a fully built request, set the signature correctly.
     *
     * @param poGoApi     the api
     * @param builder the requestenvelop builder
     */
    public static void setSignature(PoGoApi poGoApi, RequestEnvelopeOuterClass.RequestEnvelope.Builder builder) {
        long curTime = poGoApi.currentTimeMillis();
        long timestampSinceStart = curTime - poGoApi.getStartTime();

        SignatureOuterClass.Signature.Builder sigBuilder = SignatureOuterClass.Signature.newBuilder()
                .setLocationHash2(getLocationHash2(poGoApi, builder))
                .setSessionHash(ByteString.copyFrom(poGoApi.getSessionHash()))
                .setTimestamp(poGoApi.currentTimeMillis())
                .setTimestampSinceStart(timestampSinceStart);

        AuthTicketOuterClass.AuthTicket authTicket = builder.getAuthTicket();
        byte[] authTicketBA = null;
        if (authTicket != null) {
            authTicketBA = builder.getAuthTicket().toByteArray();
        }

        if (authTicketBA != null) {
            sigBuilder.setLocationHash1(getLocationHash1(poGoApi, authTicketBA, builder));
        }


        SignatureOuterClass.Signature.DeviceInfo deviceInfo = poGoApi.getDeviceInfo();
        if (deviceInfo != null) {
            sigBuilder.setDeviceInfo(deviceInfo);
        }

        SignatureOuterClass.Signature.LocationFix.Builder locationFix =
                SignatureOuterClass.Signature.LocationFix.newBuilder()
                        .setProvider("fused")
                        .setTimestampSnapshot(Math.max(timestampSinceStart - (long) (Math.random() * 300), 0))
                        .setLatitude((float) poGoApi.getLatitude())
                        .setLongitude((float) poGoApi.getLongitude())
                        .setHorizontalAccuracy((float) (Math.random() * 2.0 - 1.0))
                        .setVerticalAccuracy((float) (Math.random() * 2.0 + 10.0))
                        .setProviderStatus(3)
                        .setLocationType(1);

        sigBuilder.addLocationFix(locationFix.build());

        /*SignatureOuterClass.Signature.ActivityStatus.Builder activityStatus = SignatureOuterClass.Signature.ActivityStatus.newBuilder()
                .setUnknownStatus(true)
                .setWalking(true)
                .setStationary(true)
                .setAutomotive(true)
                .setTilting(true);

        sigBuilder.setActivityStatus(activityStatus.build());*/

        sigBuilder.setUnknown25(-8537042734809897855L);

        /*SignatureOuterClass.Signature.SensorInfo.Builder sensorInfo = SignatureOuterClass.Signature.SensorInfo.newBuilder()
                .setTimestampSnapshot(Math.max(timestampSinceStart - (long) (Math.random() * 300), 0));*/


        if (authTicketBA != null) {
            for (RequestOuterClass.Request serverRequest : builder.getRequestsList()) {
                byte[] request = serverRequest.toByteArray();
                sigBuilder.addRequestHash(getRequestHash(authTicketBA, request));
            }
        }

        byte[] uk2 = sigBuilder.build().toByteArray();
        byte[] iv = new byte[32];
        new Random().nextBytes(iv);
        byte[] encrypted = Crypto.encrypt(uk2, iv).toByteBuffer().array();
        Unknown6OuterClass.Unknown6 uk6 = Unknown6OuterClass.Unknown6.newBuilder()
                .setRequestType(6)
                .setUnknown2(Unknown2.newBuilder().setEncryptedSignature(ByteString.copyFrom(encrypted))).build();
        builder.setUnknown6(uk6);
    }

    private static byte[] getBytes(double input) {
        long rawDouble = Double.doubleToRawLongBits(input);
        return new byte[]{
                (byte) (rawDouble >>> 56),
                (byte) (rawDouble >>> 48),
                (byte) (rawDouble >>> 40),
                (byte) (rawDouble >>> 32),
                (byte) (rawDouble >>> 24),
                (byte) (rawDouble >>> 16),
                (byte) (rawDouble >>> 8),
                (byte) rawDouble
        };
    }


    private static int getLocationHash1(PoGoApi api, byte[] authTicket,
                                        RequestEnvelopeOuterClass.RequestEnvelope.Builder builder) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        StreamingXXHash32 xx32 = factory.newStreamingHash32(0x1B845238);
        xx32.update(authTicket, 0, authTicket.length);
        byte[] bytes = new byte[8 * 3];

        System.arraycopy(getBytes(api.getLatitude()), 0, bytes, 0, 8);
        System.arraycopy(getBytes(api.getLongitude()), 0, bytes, 8, 8);
        System.arraycopy(getBytes(api.getAltitude()), 0, bytes, 16, 8);

        xx32 = factory.newStreamingHash32(xx32.getValue());
        xx32.update(bytes, 0, bytes.length);
        return xx32.getValue();
    }

    private static int getLocationHash2(PoGoApi api, RequestEnvelopeOuterClass.RequestEnvelope.Builder builder) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        byte[] bytes = new byte[8 * 3];

        System.arraycopy(getBytes(api.getLatitude()), 0, bytes, 0, 8);
        System.arraycopy(getBytes(api.getLongitude()), 0, bytes, 8, 8);
        System.arraycopy(getBytes(api.getAltitude()), 0, bytes, 16, 8);

        StreamingXXHash32 xx32 = factory.newStreamingHash32(0x1B845238);
        xx32.update(bytes, 0, bytes.length);

        return xx32.getValue();
    }

    private static long getRequestHash(byte[] authTicket, byte[] request) {
        XXHashFactory factory = XXHashFactory.fastestInstance();
        StreamingXXHash64 xx64 = factory.newStreamingHash64(0x1B845238);
        xx64.update(authTicket, 0, authTicket.length);
        xx64 = factory.newStreamingHash64(xx64.getValue());
        xx64.update(request, 0, request.length);
        return xx64.getValue();
    }
}
