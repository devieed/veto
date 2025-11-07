package org.veto.shared.spider;

import lombok.Getter;
import lombok.Setter;

@Setter
@Getter
public class CapturedImage {
    private String suffix;

    private String url;

    private byte[] body;
}
