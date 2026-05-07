package net;

import java.io.Serializable;

public record ClientReady(boolean ready) implements Serializable {
}
