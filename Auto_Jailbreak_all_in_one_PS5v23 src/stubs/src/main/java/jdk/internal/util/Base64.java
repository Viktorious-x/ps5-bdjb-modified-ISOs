package jdk.internal.util;

import java.io.InputStream;
import java.nio.ByteBuffer;

/**
 * Stub for {@link java.util.Base64} class which on PS5
 * exists in a different package.
 */
public class Base64 {
    private static final Base64Encoder encoder = new Base64Encoder(java.util.Base64.getEncoder());
    private static final Base64Encoder urlEncoder = new Base64Encoder(java.util.Base64.getUrlEncoder());
    private static final Base64Encoder mimeEncoder = new Base64Encoder(java.util.Base64.getMimeEncoder());

    private static final Decoder decoder = new Decoder(java.util.Base64.getDecoder());
    private static final Decoder urlDecoder = new Decoder(java.util.Base64.getUrlDecoder());;
    private static final Decoder mimeDecoder = new Decoder(java.util.Base64.getMimeDecoder());

    private Base64() {
    }

    public static Base64Encoder getEncoder() {
        return encoder;
    }

    public static Base64Encoder getUrlEncoder() {
        return urlEncoder;
    }

    public static Base64Encoder getMimeEncoder() {
        return mimeEncoder;
    }

    public static Base64Encoder getMimeEncoder(int lineLength, byte[] lineSeparator) {
        return new Base64Encoder(java.util.Base64.getMimeEncoder(lineLength, lineSeparator));
    }

    public static Decoder getDecoder() {
        return decoder;
    }

    public static Decoder getUrlDecoder() {
        return urlDecoder;
    }

    public static Decoder getMimeDecoder() {
        return mimeDecoder;
    }

    public static class Decoder {
        private final java.util.Base64.Decoder decoder;

        private Decoder(java.util.Base64.Decoder decoder) {
            this.decoder = decoder;
        }

        public byte[] decode(byte[] src) {
            return decoder.decode(src);
        }

        public byte[] decode(String src) {
            return decoder.decode(src);
        }

        public int decode(byte[] src, byte[] dst) {
            return decoder.decode(src, dst);
        }

        public ByteBuffer decode(ByteBuffer buffer) {
            return decoder.decode(buffer);
        }

        public InputStream wrap(InputStream is) {
            return decoder.wrap(is);
        }
    }
}
