<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <!-- Attribute for dynamic page height and width -->
    <xsl:variable name="page-height" select="/visitSummary/@page-height"/>
    <xsl:variable name="page-width" select="/visitSummary/@page-width"/>

    <!-- Font families matching bundled IBM Plex Sans Arabic fonts -->
    <xsl:variable name="label-font-family">IBM Plex Sans Arabic</xsl:variable>
    <xsl:variable name="value-font-family">IBM Plex Sans Arabic Bold</xsl:variable>

    <!-- Section heading colour — ink-efficient dark grey for low-resource sites -->
    <xsl:variable name="section-heading-color">#333333</xsl:variable>

    <!-- Root template -->
    <xsl:template match="/visitSummary">
        <fo:root>
            <fo:layout-master-set>
                <fo:simple-page-master master-name="visit-summary-page"
                    page-height="{$page-height}" page-width="{$page-width}"
                    margin-top="15mm" margin-bottom="15mm"
                    margin-left="15mm" margin-right="15mm">
                    <fo:region-body margin-bottom="10mm"/>
                    <fo:region-after extent="10mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="visit-summary-page">
                <!-- Footer -->
                <fo:static-content flow-name="xsl-region-after">
                    <xsl:call-template name="footer"/>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">
                    <!-- Facility header — always rendered -->
                    <xsl:call-template name="facility-header"/>

                    <!-- Patient info — always rendered -->
                    <xsl:call-template name="patient-info"/>

                    <!-- Vitals -->
                    <xsl:if test="vitals">
                        <xsl:call-template name="vitals"/>
                    </xsl:if>

                    <!-- Diagnoses -->
                    <xsl:if test="diagnoses">
                        <xsl:call-template name="diagnoses"/>
                    </xsl:if>

                    <!-- Conditions -->
                    <xsl:if test="conditions">
                        <xsl:call-template name="conditions"/>
                    </xsl:if>

                    <!-- Lab results -->
                    <xsl:if test="labResults">
                        <xsl:call-template name="lab-results"/>
                    </xsl:if>

                    <!-- Allergies -->
                    <xsl:if test="allergies">
                        <xsl:call-template name="allergies"/>
                    </xsl:if>

                    <!-- Medications -->
                    <xsl:if test="medications">
                        <xsl:call-template name="medications"/>
                    </xsl:if>

                    <!-- Visit notes -->
                    <xsl:if test="visitNotes">
                        <xsl:call-template name="visit-notes"/>
                    </xsl:if>

                    <!-- Billing -->
                    <xsl:if test="billing">
                        <xsl:call-template name="billing"/>
                    </xsl:if>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <!-- Facility header section -->
    <xsl:template name="facility-header">
        <fo:block font-family="{$label-font-family}" margin-bottom="5mm"
            border-bottom="0.5pt solid #cccccc" padding-bottom="3mm">
            <fo:block font-size="14pt" font-weight="bold" font-family="{$value-font-family}"
                text-align="center">
                <xsl:value-of select="facilityHeader/facilityName"/>
            </fo:block>
            <fo:block font-size="9pt" text-align="center" margin-top="1mm">
                <xsl:value-of select="facilityHeader/facilityAddress"/>
            </fo:block>
            <fo:block font-size="9pt" text-align="center" margin-top="1mm">
                <xsl:value-of select="facilityHeader/facilityPhone"/>
            </fo:block>
        </fo:block>
    </xsl:template>

    <!-- Patient info section (2-column table: name, DOB, gender, visit date, patient ID) -->
    <xsl:template name="patient-info">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Patient Information
            </fo:block>
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="50%"/>
                <fo:table-column column-width="50%"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">Patient Name</fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/patientName"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">Patient ID</fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/patientId"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">Date of Birth</fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/dateOfBirth"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">Gender</fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/gender"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">Visit Date</fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/visitDate"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block/>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <!-- Vitals section -->
    <xsl:template name="vitals">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Vital Signs
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/vitals/vital">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Diagnoses section -->
    <xsl:template name="diagnoses">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Diagnoses
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/diagnoses/diagnosis">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Conditions section -->
    <xsl:template name="conditions">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Conditions
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/conditions/condition">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Lab results section -->
    <xsl:template name="lab-results">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Lab Results
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/labResults/lab">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Allergies section -->
    <xsl:template name="allergies">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Allergies
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/allergies/allergy">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Medications section -->
    <xsl:template name="medications">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Medications
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/medications/medication">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Visit notes section -->
    <xsl:template name="visit-notes">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Visit Notes
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/visitNotes/note">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Billing section -->
    <xsl:template name="billing">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                Billing
            </fo:block>
            <xsl:choose>
                <xsl:when test="/visitSummary/billing/item">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- Footer: printed-by, timestamp, system ID -->
    <xsl:template name="footer">
        <fo:block font-family="{$label-font-family}" font-size="7pt" color="#444444"
            border-top="0.5pt solid #cccccc" padding-top="1mm">
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="40%"/>
                <fo:table-column column-width="35%"/>
                <fo:table-column column-width="25%"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell>
                            <fo:block>
                                Printed by: <xsl:value-of select="/visitSummary/footer/printedBy"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                <xsl:value-of select="/visitSummary/footer/timestamp"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block text-align="right">
                                System ID: <xsl:value-of select="/visitSummary/footer/systemId"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

</xsl:stylesheet>
