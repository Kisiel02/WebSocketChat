package kisiel.jakub.websocketchat;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class ConfigDTO {

    private byte[] publicKey;

    private byte[] sessionKey;

    private int port;

    private messageType type;

    public enum messageType {
        PUBLIC_KEY,
        PUBLIC_KEY_AND_PORT,
        SESSION_KEY
    }

}
