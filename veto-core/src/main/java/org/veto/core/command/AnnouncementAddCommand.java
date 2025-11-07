package org.veto.core.command;

import lombok.Data;

@Data
public class AnnouncementAddCommand {
    private String title;

    private String content;

    private Boolean important;

    private String tags;

    private String summary;

    private Boolean status;
}
