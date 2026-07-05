package jdk.internal.util;

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.ByteBuffer;

/**
 * Stub for {@link java.util.Base64.Encoder} class which on PS5
 * exists in a different package.
 */

public class Base64Encoder {
    private final java.util.Base64.Encoder encoder;

    Base64Encoder(java.util.Base64.Encoder encoder) {
        this.encoder = encoder;
    }

    public byte[] encode(byte[] src) {
        return encoder.encode(src);
    }

    public int encode(byte[] src, byte[] dst) {
        return encoder.encode(src, dst);
    }

    public String encodeToString(byte[] src) {
        return encoder.encodeToString(src);
    }

    public ByteBuffer encode(ByteBuffer buffer) {
        return encoder.encode(buffer);
    }

    public OutputStream wrap(OutputStream os) {
        return encoder.wrap(os);
    }

    public Base64Encoder withoutPadding() {
        return new Base64Encoder(encoder.withoutPadding());
    }
}
