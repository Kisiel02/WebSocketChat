package kisiel.jakub.websocketchat.messages;

import kisiel.jakub.websocketchat.SecurityManager;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class CustomMessage {

    private String content;

    private Type type;

    private SecurityManager.BlockMode mode;

    public enum Type {
        TEXT,
        FILE_CONFIRM
    }

}
