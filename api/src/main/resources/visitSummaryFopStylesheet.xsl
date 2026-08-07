<?xml version="1.0" encoding="UTF-8"?>
<xsl:stylesheet version="1.0"
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:fo="http://www.w3.org/1999/XSL/Format">

    <xsl:output method="xml" indent="yes"/>

    <!-- Page frame from renderer attributes: XSLT 1.0 has no unit arithmetic, so
         VisitSummaryPageLayout measures and this stylesheet only interpolates. -->
    <xsl:variable name="page-height" select="/visitSummary/@page-height"/>
    <xsl:variable name="page-width"  select="/visitSummary/@page-width"/>
    <xsl:variable name="side-margin" select="/visitSummary/@side-margin"/>
    <xsl:variable name="logo-column-width"  select="/visitSummary/@logo-column-width"/>
    <xsl:variable name="logo-graphic-width" select="/visitSummary/@logo-graphic-width"/>

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
                    margin-left="{$side-margin}" margin-right="{$side-margin}">
                    <!-- The footer sizes to its own two rows of 7pt text, not to the paper,
                         so its extent and the body's clearance stay fixed while the side
                         margins scale. -->
                    <fo:region-body margin-bottom="10mm"/>
                    <fo:region-after extent="10mm"/>
                </fo:simple-page-master>
            </fo:layout-master-set>

            <fo:page-sequence master-reference="visit-summary-page">
                <fo:static-content flow-name="xsl-region-after">
                    <xsl:call-template name="footer"/>
                </fo:static-content>

                <fo:flow flow-name="xsl-region-body">
                    <!-- Sections render in document order: the renderer emits them sorted
                         by each section's configured getOrder(), with section-error and
                         section-notice elements sitting next to the section they belong to.
                         The footer element is page furniture (rendered by the static-content
                         region above) and is skipped here by its empty match template. -->
                    <xsl:apply-templates select="*"/>

                    <!-- Anchor for "Page N of M": the footer's fo:page-number-citation
                         resolves against this id on the document's last block. -->
                    <fo:block id="visit-summary-end"/>
                </fo:flow>
            </fo:page-sequence>
        </fo:root>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Facility header band. The logo cell is emitted even when the logo is
         absent, so the rest of the band does not shift or collapse. That column and its
         graphic scale with the content box (28mm and 24mm on A4) so they do not eat a
         narrow page; the other two columns stay proportional.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="facilityHeader">
        <fo:block font-family="{$label-font-family}" margin-bottom="3mm"
            border-bottom="0.5pt solid #cccccc" padding-bottom="2mm">
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="{$logo-column-width}"/>
                <fo:table-column column-width="proportional-column-width(58)"/>
                <fo:table-column column-width="proportional-column-width(42)"/>
                <fo:table-body>
                    <fo:table-row>
                        <fo:table-cell display-align="center">
                            <fo:block>
                                <xsl:if test="logoData != ''">
                                    <fo:external-graphic src="{logoData}"
                                        width="{$logo-graphic-width}" content-width="scale-down-to-fit"
                                        content-height="12mm" scaling="uniform"/>
                                </xsl:if>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell display-align="center" padding="0 2mm">
                            <xsl:if test="facilityName != ''">
                                <fo:block font-size="13pt" font-weight="bold"
                                    font-family="{$value-font-family}">
                                    <xsl:value-of select="facilityName"/>
                                </fo:block>
                            </xsl:if>
                            <xsl:if test="facilityAddress != ''">
                                <fo:block font-size="8pt" margin-top="0.5mm">
                                    <xsl:value-of select="facilityAddress"/>
                                </fo:block>
                            </xsl:if>
                            <xsl:if test="facilityPhone != ''">
                                <fo:block font-size="8pt" margin-top="0.5mm">
                                    <xsl:value-of select="facilityPhone"/>
                                </fo:block>
                            </xsl:if>
                            <!-- FOP errors on a table cell containing no block, so this empty block
                                 must stay when the facility has no details -->
                            <fo:block/>
                        </fo:table-cell>
                        <fo:table-cell display-align="center">
                            <xsl:if test="documentTitle != ''">
                                <fo:block font-size="11pt" font-weight="bold"
                                    font-family="{$value-font-family}" text-align="right"
                                    color="{$section-heading-color}">
                                    <xsl:value-of select="documentTitle"/>
                                </fo:block>
                            </xsl:if>
                            <xsl:if test="visitDate != ''">
                                <fo:block font-size="9pt" text-align="right" margin-top="0.5mm">
                                    <xsl:value-of select="visitDate"/>
                                </fo:block>
                            </xsl:if>
                            <!-- FOP errors on a table cell containing no block, so this empty block
                                 must stay when title and date are absent -->
                            <fo:block/>
                        </fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Patient information. Proportional columns so more clinical content
         fits per page on any configured page width. Field labels come from
         @lbl-* attributes set by the renderer; the age suffix is omitted
         when the age is unknown.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="patientInfo">
        <xsl:variable name="dob-with-age">
            <xsl:value-of select="dateOfBirth"/>
            <xsl:if test="age != ''">
                <xsl:text> (</xsl:text>
                <xsl:value-of select="@lbl-age"/>
                <xsl:text>: </xsl:text>
                <xsl:value-of select="age"/>
                <xsl:text>)</xsl:text>
            </xsl:if>
        </xsl:variable>
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <fo:table width="100%" table-layout="fixed">
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-column column-width="proportional-column-width(1)"/>
                <fo:table-body>
                    <fo:table-row>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-patient-name"/>
                            <xsl:with-param name="value" select="patientName"/>
                        </xsl:call-template>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-patient-id"/>
                            <xsl:with-param name="value" select="patientId"/>
                        </xsl:call-template>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-dob"/>
                            <xsl:with-param name="value" select="$dob-with-age"/>
                        </xsl:call-template>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-gender"/>
                            <xsl:with-param name="value" select="gender"/>
                        </xsl:call-template>
                    </fo:table-row>
                    <fo:table-row>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-visit-date"/>
                            <xsl:with-param name="value" select="visitDate"/>
                        </xsl:call-template>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-visit-type"/>
                            <xsl:with-param name="value" select="visitType"/>
                        </xsl:call-template>
                        <xsl:call-template name="patient-field">
                            <xsl:with-param name="label" select="@lbl-location"/>
                            <xsl:with-param name="value" select="visitLocation"/>
                        </xsl:call-template>
                        <fo:table-cell padding="0.5mm 1mm"><fo:block/></fo:table-cell>
                    </fo:table-row>
                </fo:table-body>
            </fo:table>
        </fo:block>
    </xsl:template>

    <!-- One labelled field cell of the patient block. -->
    <xsl:template name="patient-field">
        <xsl:param name="label"/>
        <xsl:param name="value"/>
        <fo:table-cell padding="0.5mm 1mm">
            <fo:block font-size="8pt" color="#444444">
                <xsl:value-of select="$label"/>
            </fo:block>
            <fo:block font-size="10pt" font-weight="bold" font-family="{$value-font-family}">
                <xsl:value-of select="$value"/>
            </fo:block>
        </fo:table-cell>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Vitals — horizontal 3-column grid of label/value pairs.
         Each <vital> carries @label and @value set by the renderer.
         Section heading comes from @heading.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="vitals">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="vital">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="33%"/>
                        <fo:table-column column-width="33%"/>
                        <fo:table-column column-width="34%"/>
                        <fo:table-body>
                            <!-- Select every 3rd vital (positions 1, 4, 7, …) to open a new row;
                                 fill the 2nd and 3rd cells via following-sibling. -->
                            <xsl:for-each select="vital[((position()-1) mod 3) = 0]">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Diagnoses — 3-column table (name, certainty, rank).
         Column headers come from @col-* attributes set by the renderer and sit
         in fo:table-header, so they repeat on every page the table spans.
         Each <diagnosis> carries @name, @certainty, @rank.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="diagnoses">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="diagnosis">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="60%"/>
                        <fo:table-column column-width="25%"/>
                        <fo:table-column column-width="15%"/>
                        <fo:table-header>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-certainty"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-rank"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <xsl:for-each select="diagnosis">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Conditions — 2-column table (name, onset date).
         Column headers come from @col-* attributes set by the renderer and sit
         in fo:table-header, so they repeat on every page the table spans.
         Each <condition> carries @name, @onset.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="conditions">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="condition">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="65%"/>
                        <fo:table-column column-width="35%"/>
                        <fo:table-header>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-onset"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <xsl:for-each select="condition">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Lab results — 4-column table (test, result, reference range, flag).
         Column headers come from @col-* attributes set by the renderer and sit
         in fo:table-header, so they repeat on every page the table spans.
         Standalone results are <lab> children; grouped panels are <lab-group heading="…">
         wrappers whose heading spans the row, followed by their member <lab> rows.
         Each <lab> carries @name, @value, @units, @range, @flag.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="labResults">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="lab or lab-group">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="40%"/>
                        <fo:table-column column-width="22%"/>
                        <fo:table-column column-width="26%"/>
                        <fo:table-column column-width="12%"/>
                        <fo:table-header>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-test"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-result"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-range"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm" border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-flag"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <!-- Grouped panels first: a heading row that spans the table, then member rows -->
                            <xsl:for-each select="lab-group">
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
                            <xsl:for-each select="lab">
                                <xsl:call-template name="lab-row">
                                    <xsl:with-param name="indent" select="'2mm'"/>
                                </xsl:call-template>
                            </xsl:for-each>
                        </fo:table-body>
                    </fo:table>
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="no-data-block"/>
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
         Column headers come from @col-* attributes set by the renderer and sit
         in fo:table-header, so they repeat on every page the table spans.
         Each <allergy> carries @allergen, @severity, @reactions.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="allergies">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="allergy">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="35%"/>
                        <fo:table-column column-width="20%"/>
                        <fo:table-column column-width="45%"/>
                        <fo:table-header>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-allergen"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-severity"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-reactions"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <xsl:for-each select="allergy">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Medications — 4-column table (medication, dosing, duration, start date).
         Column headers come from @col-* attributes set by the renderer and sit
         in fo:table-header, so they repeat on every page the table spans.
         Each <medication> carries @name, @dosing, @duration, @start.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="medications">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="medication">
                    <fo:table width="100%" table-layout="fixed">
                        <fo:table-column column-width="32%"/>
                        <fo:table-column column-width="34%"/>
                        <fo:table-column column-width="16%"/>
                        <fo:table-column column-width="18%"/>
                        <fo:table-header>
                            <fo:table-row background-color="#f5f5f5">
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-name"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-dosing"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-duration"/>
                                    </fo:block>
                                </fo:table-cell>
                                <fo:table-cell padding="1mm 2mm"
                                    border-bottom="0.5pt solid #cccccc">
                                    <fo:block font-size="8pt" font-weight="bold" color="#444444">
                                        <xsl:value-of select="@col-start"/>
                                    </fo:block>
                                </fo:table-cell>
                            </fo:table-row>
                        </fo:table-header>
                        <fo:table-body>
                            <xsl:for-each select="medication">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Visit notes: one block per note, oldest first, each with a
         provenance line (encounter datetime — provider) above the narrative.
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="visitNotes">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="note">
                    <xsl:for-each select="note">
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
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- ═══════════════════════════════════════════════════
         Billing (stub)
         ═══════════════════════════════════════════════════ -->
    <xsl:template match="billing">
        <fo:block font-family="{$label-font-family}" margin-bottom="2.5mm">
            <fo:block font-size="11pt" font-weight="bold" font-family="{$value-font-family}"
                margin-bottom="1mm" color="{$section-heading-color}"
                border-bottom="0.5pt solid #cccccc" padding-bottom="0.5mm">
                <xsl:value-of select="@heading"/>
            </fo:block>
            <xsl:choose>
                <xsl:when test="item">
                    <!-- Data will be rendered here during implementation -->
                </xsl:when>
                <xsl:otherwise>
                    <xsl:call-template name="no-data-block"/>
                </xsl:otherwise>
            </xsl:choose>
        </fo:block>
    </xsl:template>

    <!-- The footer element renders in the xsl-region-after static content on every
         page (see the root template), never in the body flow — skip it there. -->
    <xsl:template match="footer"/>

    <!-- Sections contributed by downstream modules without a template in this
         stylesheet are skipped rather than leaking raw text into the PDF. -->
    <xsl:template match="*" priority="-1"/>

    <!-- ═══════════════════════════════════════════════════
         Footer: facility name and "Page N of M" on the first row;
         printed-by, timestamp and system ID on the second.
         Rendered as fo:static-content in xsl-region-after, so it appears on
         every page. The page total comes from an fo:page-number-citation
         against the "visit-summary-end" block at the end of the flow.
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
                        <fo:table-cell number-columns-spanned="2">
                            <fo:block>
                                <xsl:value-of select="footer/facilityName"/>
                            </fo:block>
                        </fo:table-cell>
                        <fo:table-cell>
                            <fo:block text-align="right">
                                <xsl:value-of select="footer/@lbl-page"/>
                                <xsl:text> </xsl:text>
                                <fo:page-number/>
                                <xsl:text> </xsl:text>
                                <xsl:value-of select="footer/@lbl-of"/>
                                <xsl:text> </xsl:text>
                                <fo:page-number-citation ref-id="visit-summary-end"/>
                            </fo:block>
                        </fo:table-cell>
                    </fo:table-row>
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
         No-data state — shared by every section whose element has no rows.
         Deliberately distinct from the red section-error banner (grey text,
         dashed border) so a clinician can tell "nothing was recorded" apart
         from "the data could not be retrieved" on the printed page.
         Label text comes from /visitSummary/@lbl-no-data set by the renderer.
         ═══════════════════════════════════════════════════ -->
    <xsl:template name="no-data-block">
        <fo:block font-size="9pt" color="#777777" font-style="italic"
                  padding="3pt" border="0.5pt dashed #bbbbbb"
                  background-color="#fafafa">
            <xsl:value-of select="/visitSummary/@lbl-no-data"/>
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
