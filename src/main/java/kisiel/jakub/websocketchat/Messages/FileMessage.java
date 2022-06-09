package kisiel.jakub.websocketchat.Messages;

import kisiel.jakub.websocketchat.SecurityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class FileMessage {

    private byte[] chunk;

    int counter;

    SecurityManager.BlockMode blockMode;

    String fileName;

    boolean done = false;

}
