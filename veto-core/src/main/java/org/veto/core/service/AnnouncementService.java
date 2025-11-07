package org.veto.core.service;

import jakarta.annotation.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.veto.core.command.AnnouncementAddCommand;
import org.veto.core.command.AnnouncementUpdateCommand;
import org.veto.core.common.ServiceConfig;
import org.veto.core.rdbms.bean.Announcement;
import org.veto.core.rdbms.repository.AnnouncementRepository;
import org.veto.shared.Constants;
import org.veto.shared.Util;
import org.veto.shared.exception.VETO_EXCEPTION_CODE;
import org.veto.shared.exception.VetoException;

import javax.swing.plaf.InsetsUIResource;

@Service
public class AnnouncementService {

    @Resource
    private AnnouncementRepository announcementRepository;

    @Resource
    private ServiceConfig serviceConfig;

    @Transactional
    public Announcement addAnnouncement(AnnouncementAddCommand announcementAddCommand){
        Announcement announcement = new Announcement();
        announcement.setImportant(announcementAddCommand.getImportant());
        announcement.setTitle(announcementAddCommand.getTitle());
        announcement.setSummary(announcementAddCommand.getSummary());
        announcement.setTags(announcementAddCommand.getTags());
        announcement.setContent(announcementAddCommand.getContent());
        announcement.setStatus(announcementAddCommand.getStatus() == null || announcementAddCommand.getStatus());

        return announcementRepository.save(announcement);
    }

    @Transactional
    public Announcement updateAnnouncement(AnnouncementUpdateCommand announcementUpdateCommand){
        Announcement announcement = announcementRepository.findById(announcementUpdateCommand.getId()).orElseThrow(() -> new VetoException(VETO_EXCEPTION_CODE.ANNOUNCEMENT_NOT_EXISTS));
        if (Util.isNotBlank(announcementUpdateCommand.getTitle())){
            announcement.setTitle(announcementUpdateCommand.getTitle());
        }
        if (Util.isNotBlank(announcementUpdateCommand.getContent())){
            announcement.setContent(announcementUpdateCommand.getContent());
        }
        if (Util.isNotBlank(announcementUpdateCommand.getSummary())){
            announcement.setSummary(announcementUpdateCommand.getSummary());
        }
        if (announcementUpdateCommand.getStatus() != null){
            announcement.setStatus(announcementUpdateCommand.getStatus());
        }
        if (announcementUpdateCommand.getImportant() != null) {
            announcement.setImportant(announcementUpdateCommand.getImportant());
        }

        return announcementRepository.save(announcement);
    }

    public Page<Announcement> get(String word, Integer page, String type, String descParams){
        Pageable pageable = PageRequest.of(page - 1, serviceConfig.getDEFAULT_DATA_PAGE_SIZE().getVal()).withSort(Sort.by(descParams).descending());

        Announcement.TYPE tp = Announcement.TYPE.me(type);
        if (tp != null){
            return announcementRepository.findAllByStatusIsTrueAndType(pageable, tp);
        }

        return announcementRepository.findAllByStatusIsTrue(pageable);
    }
}
