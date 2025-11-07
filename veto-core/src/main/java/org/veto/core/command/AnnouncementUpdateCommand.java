package org.veto.core.command;

import lombok.Data;

@Data
public class AnnouncementUpdateCommand {

    private Long id;

    private String title;

    private String summary;

    private String content;

    private Boolean important;

    private Boolean status;
}
