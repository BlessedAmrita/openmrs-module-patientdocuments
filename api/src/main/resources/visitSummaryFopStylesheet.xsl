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
                    <xsl:apply-templates select="section-notice[@key='vitals']"/>
                    <xsl:apply-templates select="section-error[@key='vitals']"/>

                    <!-- Diagnoses -->
                    <xsl:if test="diagnoses">
                        <xsl:call-template name="diagnoses"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-error[@key='diagnoses']"/>

                    <!-- Conditions -->
                    <xsl:if test="conditions">
                        <xsl:call-template name="conditions"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-error[@key='conditions']"/>

                    <!-- Lab results -->
                    <xsl:if test="labResults">
                        <xsl:call-template name="lab-results"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-notice[@key='labResults']"/>
                    <xsl:apply-templates select="section-error[@key='labResults']"/>

                    <!-- Allergies -->
                    <xsl:if test="allergies">
                        <xsl:call-template name="allergies"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-error[@key='allergies']"/>

                    <!-- Medications -->
                    <xsl:if test="medications">
                        <xsl:call-template name="medications"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-error[@key='medications']"/>

                    <!-- Visit notes -->
                    <xsl:if test="visitNotes">
                        <xsl:call-template name="visit-notes"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-notice[@key='visitNotes']"/>
                    <xsl:apply-templates select="section-error[@key='visitNotes']"/>

                    <!-- Billing -->
                    <xsl:if test="billing">
                        <xsl:call-template name="billing"/>
                    </xsl:if>
                    <xsl:apply-templates select="section-error[@key='billing']"/>
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
            <xsl:if test="facilityHeader/facilityName != ''">
                <fo:block font-size="14pt" font-weight="bold" font-family="{$value-font-family}"
                    text-align="center">
                    <xsl:value-of select="facilityHeader/facilityName"/>
                </fo:block>
            </xsl:if>
            <xsl:if test="facilityHeader/facilityAddress != ''">
                <fo:block font-size="9pt" text-align="center" margin-top="1mm">
                    <xsl:value-of select="facilityHeader/facilityAddress"/>
                </fo:block>
            </xsl:if>
            <xsl:if test="facilityHeader/facilityPhone != ''">
                <fo:block font-size="9pt" text-align="center" margin-top="1mm">
                    <xsl:value-of select="facilityHeader/facilityPhone"/>
                </fo:block>
            </xsl:if>
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
         Conditions — 2-column table (name, onset date).
         Column headers come from conditions/@col-* attributes set by the renderer.
         Each <condition> carries @name, @onset.
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
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="65%"/>
                        <fo:table-column column-width="35%"/>
                        <fo:table-body>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="conditions/@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="conditions/@col-onset"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <xsl:for-each select="conditions/condition">
                                <fo:table-row>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@name"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@onset"/>
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
         Lab results — 4-column table (test, result, reference range, flag).
         Column headers come from labResults/@col-* attributes set by the renderer.
         Standalone results are <lab> children; grouped panels are <lab-group heading="…">
         wrappers whose heading spans the row, followed by their member <lab> rows.
         Each <lab> carries @name, @value, @units, @range, @flag.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="lab-results">
        <fo:block font-family="{$label-font-family}" margin-bottom="4mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="2mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="1mm">
                <xsl:value-of select="labResults/@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="labResults/lab or labResults/lab-group">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="40%"/>
                        <fo:table-column column-width="22%"/>
                        <fo:table-column column-width="26%"/>
                        <fo:table-column column-width="12%"/>
                        <fo:table-body>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="labResults/@col-test"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="labResults/@col-result"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="labResults/@col-range"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="labResults/@col-flag"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <!-- Grouped panels first: a heading row that spans the table, then member rows -->
                            <xsl:for-each select="labResults/lab-group">
                                <fo:table-row>
                                    <fo:table-cell number-columns-spanned="4" padding="1mm 2mm"
                                        background-color="#fafafa" border-bottom="0.25pt solid #eeeeee">
                                        <fo:block font-size="9pt" font-weight="bold" color="#444444">
                                            <xsl:value-of select="@heading"/>
                                        </fo:block>
                                    </fo:table-cell>
                                </fo:table-row>
                                <xsl:for-each select="lab">
                                    <xsl:call-template name="lab-row">
                                        <xsl:with-param name="indent" select="'4mm'"/>
                                    </xsl:call-template>
                                </xsl:for-each>
                            </xsl:for-each>
                            <!-- Standalone results -->
                            <xsl:for-each select="labResults/lab">
                                <xsl:call-template name="lab-row">
                                    <xsl:with-param name="indent" select="'2mm'"/>
                                </xsl:call-template>
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

    <!-- Single lab result row. Result cell shows value plus units when present. -->
    <xsl:template name="lab-row">
        <xsl:param name="indent" select="'2mm'"/>
        <fo:table-row>
            <fo:table-cell padding="1mm 2mm" padding-left="{$indent}">
                <fo:block font-size="9pt">
                    <xsl:value-of select="@name"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding="1mm 2mm">
                <fo:block font-size="9pt">
                    <xsl:value-of select="@value"/>
                    <xsl:if test="@units != ''">
                        <xsl:text> </xsl:text>
                        <xsl:value-of select="@units"/>
                    </xsl:if>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding="1mm 2mm">
                <fo:block font-size="9pt">
                    <xsl:value-of select="@range"/>
                </fo:block>
            </fo:table-cell>
            <fo:table-cell padding="1mm 2mm">
                <fo:block font-size="9pt">
                    <xsl:value-of select="@flag"/>
                </fo:block>
            </fo:table-cell>
        </fo:table-row>
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
         Medications — 4-column table (medication, dosing, duration, start date).
         Column headers come from medications/@col-* attributes set by the renderer.
         Each <medication> carries @name, @dosing, @duration, @start.
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
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="32%"/>
                        <fo:table-column column-width="34%"/>
                        <fo:table-column column-width="16%"/>
                        <fo:table-column column-width="18%"/>
                        <fo:table-body>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="medications/@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="medications/@col-dosing"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="medications/@col-duration"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="medications/@col-start"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                            <xsl:for-each select="medications/medication">
                                <fo:table-row>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@name"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@dosing"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@duration"/>
                                        </fo:block>
                                    </fo:table-cell>
                                    <fo:table-cell padding="1mm 2mm">
                                        <fo:block font-size="9pt">
                                            <xsl:value-of select="@start"/>
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
         Visit notes: one block per note, oldest first, each with a
         provenance line (encounter datetime — provider) above the narrative.
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
                    <xsl:for-each select="visitNotes/note">
                        <fo:block margin-bottom="2mm">
                            <fo:block font-size="8pt" color="#666666" font-style="italic">
                                <xsl:value-of select="@datetime"/>
                                <xsl:text> — </xsl:text>
                                <xsl:value-of select="@provider"/>
                            </fo:block>
                            <fo:block font-size="9pt" linefeed-treatment="preserve">
                                <xsl:value-of select="."/>
                            </fo:block>
                        </fo:block>
                    </xsl:for-each>
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
    
    <!-- ═══════════════════════════════════════════════════
         Section error fallback
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="section-error">
        <fo:block font-size="9pt" font-style="italic" color="#CC0000"
                  space-before="6pt" space-after="6pt"
                  padding="4pt" border="0.5pt solid #CC0000"
                  background-color="#FFF0F0">
            <!-- Bundled Plex Sans Arabic has no U+26A0, so a warning-sign ⚠ glyph renders as "#" -->
            <fo:inline font-weight="bold">! </fo:inline>
            <xsl:value-of select="@key"/>
            <xsl:text>: </xsl:text>
            <xsl:value-of select="@message"/>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Section notice — partial data loaded (non-fatal warning)
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="section-notice">
        <fo:block font-size="9pt" font-style="italic" color="#B8860B"
                  space-before="6pt" space-after="6pt"
                  padding="4pt" border="0.5pt solid #B8860B"
                  background-color="#FFF8E1">
            <!-- Bundled Plex Sans Arabic has no U+26A0, so a warning-sign ⚠ glyph renders as "#" -->
            <fo:inline font-weight="bold">! </fo:inline>
            <xsl:value-of select="@message"/>
        </fo:block>
    </xsl:template>

</xsl:stylesheet>
