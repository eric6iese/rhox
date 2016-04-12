package com.jjstk.jclasspath;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.aether.transfer.AbstractTransferListener;
import org.eclipse.aether.transfer.MetadataNotFoundException;
import org.eclipse.aether.transfer.TransferEvent;
import org.eclipse.aether.transfer.TransferResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A simplistic transfer listener that logs uploads/downloads to the console.
 * <br/>
 * Base form taken from:<br/>
 * <a href="https://github.com/eclipse/aether-demo">aether-demo</a>
 */
final class ConsoleTransferListener extends AbstractTransferListener {

    private final Logger LOG = LoggerFactory.getLogger(ConsoleRepositoryListener.class);

    private Map<TransferResource, Long> downloads = new ConcurrentHashMap<>();

    private int lastLength;

    @Override
    public void transferInitiated(TransferEvent event) {
        String message = event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploading" : "Downloading";

        LOG.info(message + ": " + event.getResource().getRepositoryUrl() + event.getResource().getResourceName());
    }

    @Override
    public void transferProgressed(TransferEvent event) {
        TransferResource resource = event.getResource();
        downloads.put(resource, event.getTransferredBytes());

        StringBuilder buffer = new StringBuilder(64);

        downloads.forEach((k, v) -> {
            long total = k.getContentLength();
            long complete = v;
            buffer.append(getStatus(complete, total)).append("  ");
        });

        int pad = lastLength - buffer.length();
        lastLength = buffer.length();
        pad(buffer, pad);

        LOG.debug("{}", buffer);
    }

    private String getStatus(long complete, long total) {
        if (total >= 1024) {
            return toKB(complete) + "/" + toKB(total) + " KB ";
        } else if (total >= 0) {
            return complete + "/" + total + " B ";
        } else if (complete >= 1024) {
            return toKB(complete) + " KB ";
        } else {
            return complete + " B ";
        }
    }

    private void pad(StringBuilder buffer, int spaces) {
        String block = "                                        ";
        while (spaces > 0) {
            int n = Math.min(spaces, block.length());
            buffer.append(block, 0, n);
            spaces -= n;
        }
    }

    @Override
    public void transferSucceeded(TransferEvent event) {
        transferCompleted(event);

        TransferResource resource = event.getResource();
        long contentLength = event.getTransferredBytes();
        if (contentLength >= 0) {
            String type = (event.getRequestType() == TransferEvent.RequestType.PUT ? "Uploaded" : "Downloaded");
            String len = contentLength >= 1024 ? toKB(contentLength) + " KB" : contentLength + " B";

            String throughput = "";
            long duration = System.currentTimeMillis() - resource.getTransferStartTime();
            if (duration > 0) {
                long bytes = contentLength - resource.getResumeOffset();
                DecimalFormat format = new DecimalFormat("0.0", new DecimalFormatSymbols(Locale.ENGLISH));
                double kbPerSec = (bytes / 1024.0) / (duration / 1000.0);
                throughput = " at " + format.format(kbPerSec) + " KB/sec";
            }

            LOG.info(type + ": " + resource.getRepositoryUrl() + resource.getResourceName() + " (" + len + throughput + ")");
        }
    }

    @Override
    public void transferFailed(TransferEvent event) {
        transferCompleted(event);

        if (!(event.getException() instanceof MetadataNotFoundException)) {
            LOG.error("Transfer failed!", event.getException());
        }
    }

    private void transferCompleted(TransferEvent event) {
        downloads.remove(event.getResource());

        StringBuilder buffer = new StringBuilder(64);
        pad(buffer, lastLength);
        LOG.debug("{}", buffer);
    }

    public void transferCorrupted(TransferEvent event) {
        LOG.error("Transfer is corrupted!", event.getException());
    }

    protected long toKB(long bytes) {
        return (bytes + 1023) / 1024;
    }

}
