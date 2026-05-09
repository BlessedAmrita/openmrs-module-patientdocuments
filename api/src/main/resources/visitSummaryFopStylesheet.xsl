<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <!-- Page dimensions from renderer attributes -->
    <xsl:variable name="page-height" select="/visitSummary/@page-height"/>
    <xsl:variable name="page-width"  select="/visitSummary/@page-width"/>

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
                <fo:static-content flow-name="xsl-region-after">
                    <xsl:call-template name="footer"/>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">
                    <!-- Facility header — always rendered -->
                    <xsl:call-template name="facility-header"/>

                    <!-- Patient info — always rendered -->
                    <xsl:call-template name="patient-info"/>

                    <!-- Vitals (element always present; inner check drives "None recorded") -->
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

    <!-- ═══════════════════════════════════════════════════
         Facility header
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="facility-header">
        <fo:block font-family="{$label-font-family}" margin-bottom="5mm"
            border-bottom="0.5pt solid #cccccc" padding-bottom="3mm">
            <!-- Logo: rendered only when logoData element is non-empty -->
            <xsl:if test="facilityHeader/logoData != ''">
                <fo:block text-align="center" margin-bottom="2mm">
                    <fo:external-graphic src="{facilityHeader/logoData}"
                        content-height="15mm" scaling="uniform"/>
                </fo:block>
            </xsl:if>
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

    <!-- ═══════════════════════════════════════════════════
         Patient information (2-column table)
         All field labels come from patientInfo/@lbl-* attributes set by the renderer.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="patient-info">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="patientInfo/@heading"/>
            </fo:block>
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="50%"/>
                <fo:table-column column-width="50%"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-patient-name"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/patientName"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-patient-id"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/patientId"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-dob"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/dateOfBirth"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-gender"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/gender"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-visit-date"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/visitDate"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-visit-type"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/visitType"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                    <fo:table-row>
                        <fo:table-cell padding="1mm">
                            <fo:block font-size="8pt" color="#444444">
                                <xsl:value-of select="patientInfo/@lbl-location"/>
                            </fo:block>
                            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                                <xsl:value-of select="patientInfo/visitLocation"/>
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

    <!-- ═══════════════════════════════════════════════════
         Vitals — horizontal 3-column grid of label/value pairs.
         Each <vital> carries @label and @value set by the renderer.
         Section heading comes from vitals/@heading.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="vitals">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="vitals/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="vitals/vital">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="33%"/>
                        <fo:table-column column-width="33%"/>
                        <fo:table-column column-width="34%"/>
                        <fo:table-body>
                            <!-- Select every 3rd vital (positions 1, 4, 7, …) to open a new row;
                                 fill the 2nd and 3rd cells via following-sibling. -->
                            <xsl:for-each select="vitals/vital[((position()-1) mod 3) = 0]">
                                <fo:table-row>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="8pt" color="#444444">
                                            <xsl:value-of select="@label"/>
                                        </fo:block>
                                        <fo:block font-size="10pt" font-weight="bold"
                                            font-family="{$value-font-family}">
                                            <xsl:value-of select="@value"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <xsl:choose>
                                        <xsl:when test="following-sibling::vital[1]">
                                            <fo:table-cell padding="1mm 2mm">
                                                <fo:block font-size="8pt" color="#444444">
                                                    <xsl:value-of select="following-sibling::vital[1]/@label"/>
                                                </fo:block>
                                                <fo:block font-size="10pt" font-weight="bold"
                                                    font-family="{$value-font-family}">
                                                    <xsl:value-of select="following-sibling::vital[1]/@value"/>
                                                </fo:block>
                                            </fo:table-cell>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <fo:table-cell><fo:block/></fo:table-cell>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                    <xsl:choose>
                                        <xsl:when test="following-sibling::vital[2]">
                                            <fo:table-cell padding="1mm 2mm">
                                                <fo:block font-size="8pt" color="#444444">
                                                    <xsl:value-of select="following-sibling::vital[2]/@label"/>
                                                </fo:block>
                                                <fo:block font-size="10pt" font-weight="bold"
                                                    font-family="{$value-font-family}">
                                                    <xsl:value-of select="following-sibling::vital[2]/@value"/>
                                                </fo:block>
                                            </fo:table-cell>
                                        </xsl:when>
                                        <xsl:otherwise>
                                            <fo:table-cell><fo:block/></fo:table-cell>
                                        </xsl:otherwise>
                                    </xsl:choose>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Diagnoses — 3-column table (name, certainty, rank).
         Column headers come from diagnoses/@col-* attributes set by the renderer.
         Each <diagnosis> carries @name, @certainty, @rank.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="diagnoses">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="diagnoses/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="diagnoses/diagnosis">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="60%"/>
                        <fo:table-column column-width="25%"/>
                        <fo:table-column column-width="15%"/>
                        <fo:table-body>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="diagnoses/@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="diagnoses/@col-certainty"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="diagnoses/@col-rank"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <xsl:for-each select="diagnoses/diagnosis">
                                <fo:table-row>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@name"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@certainty"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@rank"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Conditions (stub — data supplied by future section provider)
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="conditions">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="conditions/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="conditions/condition">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Lab results (stub)
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="lab-results">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="labResults/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="labResults/lab">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Allergies — 3-column table (allergen, severity, reactions).
         Column headers come from allergies/@col-* attributes set by the renderer.
         Each <allergy> carries @allergen, @severity, @reactions.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="allergies">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="allergies/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="allergies/allergy">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="35%"/>
                        <fo:table-column column-width="20%"/>
                        <fo:table-column column-width="45%"/>
                        <fo:table-body>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="allergies/@col-allergen"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="allergies/@col-severity"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="allergies/@col-reactions"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <xsl:for-each select="allergies/allergy">
                                <fo:table-row>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@allergen"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@severity"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@reactions"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Medications (stub)
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="medications">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="medications/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="medications/medication">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Visit notes (stub)
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="visit-notes">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="visitNotes/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="visitNotes/note">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Billing (stub)
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="billing">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="billing/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="billing/item">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <fo:block font-size="9pt" color="#999999" font-style="italic">None recorded</fo:block>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Footer: printed-by, timestamp, system ID.
         Label text comes from footer/@lbl-* attributes set by the renderer.
         ═══════════════════════════════════════════════════ -->
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
                                <xsl:value-of select="footer/@lbl-printed-by"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="footer/printedBy"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block>
                                <xsl:value-of select="footer/timestamp"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block text-align="right">
                                <xsl:value-of select="footer/@lbl-system-id"/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="footer/systemId"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

</xsl:stylesheet>
