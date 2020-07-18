package link.infra.jumploader.resolution.download.verification;

import java.io.InputStream;

public interface HashVerifierProvider {
	InputStream getVerifier(InputStream src);
}
