package net.structs;

import java.io.Serializable;

public record RemotePromptCancel(long promptId) implements Serializable {
}
