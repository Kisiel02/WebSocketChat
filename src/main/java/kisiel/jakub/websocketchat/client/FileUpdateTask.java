package kisiel.jakub.websocketchat.client;

import javafx.concurrent.Task;
import kisiel.jakub.websocketchat.SecurityManager;
import kisiel.jakub.websocketchat.messages.FileMessage;
import lombok.AllArgsConstructor;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;

@AllArgsConstructor
public class FileUpdateTask<T> extends Task<T> {

    private final int bufferSize;

    private File file;

    private SecurityManager securityManager;

    private SecurityManager.BlockMode mode;

    private ConnectionManager connectionManager;

    @Override
    protected T call() {
        int count = 0;
        int read;
        long alreadyRead = 0;
        byte[] buffer = new byte[bufferSize];
        try (BufferedInputStream in = new BufferedInputStream(new FileInputStream(file))) {
            while ((read = in.read(buffer)) > 0) {
                FileMessage fileMessage = new FileMessage();
                if (read != bufferSize) {
                    //last chunk
                    fileMessage.setDone(true);
                    byte[] lastChunk = new byte[read];
                    System.arraycopy(buffer, 0, lastChunk, 0, read);
                    fileMessage.setChunk(this.securityManager.encryptFileWithSessionKey(lastChunk, mode));
                } else {
                    fileMessage.setDone(false);
                    fileMessage.setChunk(this.securityManager.encryptFileWithSessionKey(buffer, mode));
                }

                fileMessage.setCounter(count);
                fileMessage.setBlockMode(mode);
                fileMessage.setFileName(file.getName());
                alreadyRead += read;

                count++;
                connectionManager.sendChunk(fileMessage);

                updateProgress(alreadyRead, file.length());
            }

        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }
}
