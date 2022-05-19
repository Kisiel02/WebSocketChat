package kisiel.jakub.websocketchat.server.textMessage;

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

    public enum Type {
        TEXT,
        CONNECT
    }

}
