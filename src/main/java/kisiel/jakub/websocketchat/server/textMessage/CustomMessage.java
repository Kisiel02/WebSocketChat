package kisiel.jakub.websocketchat.server.textMessage;

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

    private String text;

    private Type type;

    private SecurityManager.blockMode mode;

    public enum Type {
        TEXT,
        CONNECT,
        INFO,
        PUBLIC_KEY
    }

}
