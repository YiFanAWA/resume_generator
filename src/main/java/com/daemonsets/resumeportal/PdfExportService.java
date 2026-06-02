package com.daemonsets.resumeportal;

import com.daemonsets.resumeportal.models.Education;
import com.daemonsets.resumeportal.models.Job;
import com.daemonsets.resumeportal.models.UserProfile;
import com.lowagie.text.*;
import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import com.mysql.cj.x.protobuf.MysqlxCursor;
import com.lowagie.text.DocumentException;
import org.springframework.data.domain.Page;
import org.springframework.security.core.parameters.P;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class PdfExportService {
    public byte[] generatePdf(UserProfile profile) throws DocumentException, IOException{
        Document document = new Document(PageSize.A4, 50, 50, 50, 50);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();

        PdfWriter.getInstance(document, baos);
        document.open();

        BaseFont chineseFont = BaseFont.createFont("STSong-Light", "UniGB-UCS2-H", BaseFont.EMBEDDED);
        Font titleFont = new Font(chineseFont, 24, Font.BOLD);
        Font subtitleFont = new Font(chineseFont, 14, Font.BOLD);
        Font normalFont = new Font(chineseFont, 10, Font.NORMAL);
        Font sectionFont = new Font(chineseFont, 12, Font.BOLD);

        addHeader(document, profile, titleFont, subtitleFont, normalFont);
        addPersonalProfile(document, profile, sectionFont, normalFont);
        addWorkExperience(document, profile, sectionFont, normalFont);
        addSkills(document, profile, sectionFont, normalFont);
        addEducation(document, profile, sectionFont, normalFont);

        document.close();
        return baos.toByteArray();

    }
    private void addHeader(Document document, UserProfile profile, Font titleFont, Font subtitleFont, Font normalFont) throws DocumentException{
        Paragraph name = new Paragraph(profile.getFirstName() + " " + profile.getLastName(), titleFont);
        name .setAlignment(Element.ALIGN_CENTER);
        document.add(name);

        if (profile.getDesignation() != null && !profile.getDesignation().isEmpty()){
            Paragraph designation = new Paragraph(profile.getDesignation(), subtitleFont);
            designation.setAlignment(Element.ALIGN_CENTER);
            designation.setSpacingAfter(10);
            document.add(designation);
        }
        Paragraph content = new Paragraph();
        content.setAlignment(Element.ALIGN_CENTER);
        content.setSpacingAfter(15);

        if (profile.getEmail() != null){
            content.add(new Chunk("Email: "+profile.getEmail(), normalFont));
        }
        if (profile.getPhone() != null){
            content.add(new Chunk("Phone: "+profile.getPhone(), normalFont));
        }
        document.add(content);

        document.add(new Paragraph("_____________________________________________________________________________ ", normalFont));
    }
    private void addPersonalProfile(Document document, UserProfile profile, Font sectionFont, Font normalFont) throws DocumentException{
        if (profile.getSummary() != null && !profile.getSummary().isEmpty()){
            Paragraph section = new Paragraph("Personal Profile", sectionFont);
            section.setSpacingBefore(10);
            section.setSpacingAfter(5);
            document.add(section);

            Paragraph summary = new Paragraph(profile.getSummary(), normalFont);
            summary.setSpacingAfter(10);
            document.add(summary);
        }
    }
    private void addWorkExperience(Document document, UserProfile profile, Font sectionFont, Font normalFont) throws DocumentException{
    if (profile.getJobs() != null && !profile.getJobs().isEmpty()){
        Paragraph section = new Paragraph("Work Experience", sectionFont);
        section.setSpacingBefore(10);
        section.setSpacingAfter(5);
        document.add(section);

        for (Job job : profile.getJobs()) {
            Paragraph jobTitle = new Paragraph();
            jobTitle.add(new Chunk(job.getDesignation(), normalFont));
            jobTitle.add(new Chunk(" at ", new Font(normalFont.getFamily(), normalFont.getSize(), Font.ITALIC)));
            jobTitle.add(new Chunk(job.getCompany(), normalFont));
            document.add(jobTitle);

            Paragraph dates = new Paragraph(job.getFormattedStartDate() + " - "+(job.isCurrentJob() ? "Present" : job.getFormattedEndDate()), new Font(normalFont.getFamily(), 9, Font.ITALIC));
            dates.setSpacingAfter(3);
            document.add(dates);

            if (job.getResponsibilities() != null && !job.getResponsibilities().isEmpty()){
                for (String responsibility : job.getResponsibilities()){
                    Paragraph resp = new Paragraph("- "+responsibility, normalFont);
                    resp.setIndentationLeft(10);
                    document.add(resp);
                }
            }
            document.add(new Paragraph("_____________________________________________________________________________ ", normalFont));
        }
    }
    }
    private void addSkills(Document document, UserProfile profile, Font sectionFont, Font normalFont) throws DocumentException{
        if (profile.getSkills() != null && !profile.getSkills().isEmpty()){
            Paragraph section = new Paragraph("Skills", sectionFont);
            section.setSpacingBefore(10);
            section.setSpacingAfter(5);
            document.add(section);

            Paragraph skills = new Paragraph(String.join(", ", profile.getSkills()), normalFont);
            skills.setSpacingAfter(10);
            document.add(skills);
            }
        }
    private void addEducation(Document document, UserProfile profile, Font sectionFont, Font normalFont) throws DocumentException{
        if (profile.getEducations() != null && !profile.getEducations().isEmpty()){
            Paragraph section = new Paragraph("Education", sectionFont);
            section.setSpacingBefore(10);
            section.setSpacingAfter(5);
            document.add(section);

            for (Education education : profile.getEducations()){
                Paragraph college = new Paragraph(education.getCollege(), normalFont);
                document.add(college);

                if (education.getQualification() !=null){
                    Paragraph qualifcation = new Paragraph(education.getQualification(), new Font(normalFont.getFamily(), 9, Font.ITALIC));
                    document.add(qualifcation);
                }
                if (education.getSummary() != null){
                    Paragraph summary = new Paragraph(education.getSummary(), normalFont);
                    summary.setSpacingAfter(5);
                    document.add(summary);
                }
            }
        }
    }
}
