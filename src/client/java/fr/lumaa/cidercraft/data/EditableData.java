package fr.lumaa.cidercraft.data;

import java.io.File;

public interface EditableData {
    /**
     * Encodes this class into a JSON string
     */
    String encodeJson();
    void decodeJson(File fromFile);
}
