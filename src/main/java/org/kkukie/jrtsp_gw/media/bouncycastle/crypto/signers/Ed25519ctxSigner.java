package org.kkukie.jrtsp_gw.media.bouncycastle.crypto.signers;

import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.CipherParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.Signer;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.Ed25519PrivateKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.crypto.params.Ed25519PublicKeyParameters;
import org.kkukie.jrtsp_gw.media.bouncycastle.math.ec.rfc8032.Ed25519;
import org.kkukie.jrtsp_gw.media.bouncycastle.util.Arrays;

import java.io.ByteArrayOutputStream;

public class Ed25519ctxSigner
    implements Signer
{
    private final Buffer buffer = new Buffer();
    private final byte[] context;

    private boolean forSigning;
    private Ed25519PrivateKeyParameters privateKey;
    private Ed25519PublicKeyParameters publicKey;

    public Ed25519ctxSigner(byte[] context)
    {
        this.context = Arrays.clone(context);
    }

    public void init(boolean forSigning, CipherParameters parameters)
    {
        this.forSigning = forSigning;

        if (forSigning)
        {
            // Allow AsymmetricCipherKeyPair to be a CipherParameters?

            this.privateKey = (Ed25519PrivateKeyParameters)parameters;
            this.publicKey = privateKey.generatePublicKey();
        }
        else
        {
            this.privateKey = null;
            this.publicKey = (Ed25519PublicKeyParameters)parameters;
        }

        reset();
    }

    public void update(byte b)
    {
        buffer.write(b);
    }

    public void update(byte[] buf, int off, int len)
    {
        buffer.write(buf, off, len);
    }

    public byte[] generateSignature()
    {
        if (!forSigning || null == privateKey)
        {
            throw new IllegalStateException("Ed25519ctxSigner not initialised for signature generation.");
        }

        return buffer.generateSignature(privateKey, publicKey, context);
    }

    public boolean verifySignature(byte[] signature)
    {
        if (forSigning || null == publicKey)
        {
            throw new IllegalStateException("Ed25519ctxSigner not initialised for verification");
        }

        return buffer.verifySignature(publicKey, context, signature);
    }

    public void reset()
    {
        buffer.reset();
    }

    private static class Buffer extends ByteArrayOutputStream
    {
        synchronized byte[] generateSignature(Ed25519PrivateKeyParameters privateKey, Ed25519PublicKeyParameters publicKey, byte[] ctx)
        {
            byte[] signature = new byte[Ed25519PrivateKeyParameters.SIGNATURE_SIZE];
            privateKey.sign(Ed25519.Algorithm.Ed25519ctx, publicKey, ctx, buf, 0, count, signature, 0);
            reset();
            return signature;
        }

        synchronized boolean verifySignature(Ed25519PublicKeyParameters publicKey, byte[] ctx, byte[] signature)
        {
            byte[] pk = publicKey.getEncoded();
            boolean result = Ed25519.verify(signature, 0, pk, 0, ctx, buf, 0, count);
            reset();
            return result;
        }

        public synchronized void reset()
        {
            Arrays.fill(buf, 0, count, (byte)0);
            this.count = 0;
        }
    }
}
