<?xml version="1.0"?>
<!-- 
    Style sheet to transform RDF descriptions to HTML Forms
-->
<xsl:stylesheet version="1.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#">

	<!-- xsl:import href="rdf2html-functions.xsl"/-->
	
	<xsl:output media-type="text/xhtml" encoding="UTF-8" indent="yes" omit-xml-declaration="yes"/>

	<xsl:strip-space elements="*"/>

	<xsl:param name="action">edit</xsl:param>
	<xsl:param name="language">en</xsl:param>
	
	<xsl:variable name="defaultLangs">
		<option value="en">en</option>
		<option value="es">es</option>
	</xsl:variable>
	
	<xsl:template match="rdf:RDF">
		<form name="editform">
			<script type="text/javascript">
				<xsl:text disable-output-escaping="yes"><![CDATA[ //<![CDATA[  ]]></xsl:text>
					rhizomik.SemanticForms.getGenericProperties(rhz);
				<xsl:text disable-output-escaping="yes"><![CDATA[//]]]]><![CDATA[>]]></xsl:text>
			</script>
			
			<xsl:for-each select="child::*">
				<xsl:sort select="@rdf:about" order="ascending"/>
				<xsl:call-template name="rdfDescription"/>
				<xsl:if test="not(local-name()='Description') or *[not(name()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label')] |
							@*[not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label' or local-name()='about')]">
					<div id="actions">
						<xsl:choose>
							<xsl:when test="$action='test'">
								<input type="button" value="test" 
									onclick="console.log(rhizomik.SemanticForms.formToNTriples(editform));"/>
							</xsl:when>
							<xsl:when test="$action='edit'">
								<input type="button" value="edit" 
									onclick="rhz.putRDF('{@rdf:about}', rhizomik.SemanticForms.formToNTriples(editform), 'application/n-triples');"/>
							</xsl:when>
							<xsl:when test="$action='new'">
								<input type="button" value="new" 
									onclick="rhz.postRDF(rhizomik.SemanticForms.formToNTriples(editform), 'application/n-triples');"/>
							</xsl:when>
						</xsl:choose>
						<input type="button" value="cancel" onClick="window.location.reload();"/>
					</div>
				</xsl:if>
			</xsl:for-each>
		</form>
	</xsl:template>
  	
	<xsl:template name="rdfDescription">
		<xsl:choose>
			<!-- RDF Description that contains more than labels -->
			<xsl:when test="not(local-name()='Description') or *[not(name()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label')] |
							@*[not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label' or local-name()='about')]">
				<xsl:variable name="types">
					<xsl:text>new Array(</xsl:text>
					<xsl:if test="not(local-name()='Description') and not(@rdf:parseType='Resource')">
						<xsl:text>'</xsl:text><xsl:value-of select="concat(namespace-uri(), local-name())"/><xsl:text>'</xsl:text>
						<xsl:if test="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
							<xsl:text>,</xsl:text>
						</xsl:if>
					</xsl:if>
					<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
						<xsl:text>'</xsl:text><xsl:value-of select="@rdf:resource"/><xsl:text>'</xsl:text>
						<xsl:if test="following-sibling::*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
							<xsl:text>,</xsl:text>
						</xsl:if>
					</xsl:for-each>
					<xsl:text>)</xsl:text>
				</xsl:variable>
				<div id="description"><table>
					<xsl:call-template name="identifier"/>
					<xsl:call-template name="types"/>
					<xsl:call-template name="attributes"/>
					<xsl:call-template name="properties">
						<xsl:with-param name="domainTypes" select="$types"/>
					</xsl:call-template>
					<tr id="{@rdf:ID|@rdf:about}"><td colspan="2" style="text-align:right"><a href="javascript:rhizomik.SemanticForms.addProperty('{@rdf:ID|@rdf:about}',{$types})">+</a></td></tr>
				</table></div>
			</xsl:when>
			<xsl:otherwise><!-- Ignore RDF Descriptions with just labels --></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="embeddedRdfDescription">
	<xsl:param name="parentResourceArray"/>
		<xsl:variable name="types">
			<xsl:text>new Array(</xsl:text>
			<xsl:if test="not(local-name()='Description') and not(@rdf:parseType='Resource')">
				<xsl:text>'</xsl:text><xsl:value-of select="concat(namespace-uri(), local-name())"/><xsl:text>'</xsl:text>
				<xsl:if test="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
					<xsl:text>,</xsl:text>
				</xsl:if>
			</xsl:if>
			<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
				<xsl:text>'</xsl:text><xsl:value-of select="@rdf:resource"/><xsl:text>'</xsl:text>
				<xsl:if test="following-sibling::*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
					<xsl:text>,</xsl:text>
				</xsl:if>
			</xsl:for-each>
			<xsl:text>)</xsl:text>
		</xsl:variable>
		<xsl:choose>
			<!-- Embedded RDF Description that contains more than labels -->
			<xsl:when test="*[not(name()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label')] |
							@*[not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label' or local-name()='about')]">
				<table>
					<xsl:call-template name="identifier"/>
					<xsl:call-template name="types"/>
					<xsl:call-template name="attributes"/>
					<xsl:call-template name="properties">
						<xsl:with-param name="domainTypes" select="$types"/>
					</xsl:call-template>
					<tr id="_:{generate-id(.)}"><td colspan="2" style="text-align:right"><a href="javascript:rhizomik.SemanticForms.addProperty('_:{generate-id(.)}',{$types})">+</a></td></tr>
				</table>
			</xsl:when>
			<!-- Embeded RDF Description with just labels, just take the URI -->
			<xsl:when test="parent::*[not(name()='rdf:RDF')]">
				<xsl:call-template name="autocomplete">
					<xsl:with-param name="domainTypes" select="$parentResourceArray"/>
					<xsl:with-param name="propertyURI" select="concat(namespace-uri(parent::*),local-name(parent::*))"/>
					<xsl:with-param name="valueURI" select="@rdf:about"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise><!-- Ignore other --></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="identifier">
		<xsl:choose>
			<xsl:when test="@rdf:ID|@rdf:about">
				<tr>
					<th colspan="2">
						<xsl:variable name="identifier">
							<xsl:value-of select="@rdf:ID|@rdf:about"/>
						</xsl:variable>
						<input style="text-align:center" type="text" name="{namespace-uri(@*)}{local-name(@*)}" value="{$identifier}"/>
					</th>
				</tr>
			</xsl:when>
			<xsl:otherwise> <!-- Anonymous resource, generate anonymous ID -->
				<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" value="_:{generate-id(.)}"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="types">
		<xsl:variable name="resourceArray">
			<xsl:text>new Array('http://www.w3.org/2000/01/rdf-schema#Resource')</xsl:text>
		</xsl:variable>
		<!-- embedded rdf:type -->
		<xsl:if test="not(local-name()='Description') and not(@rdf:parseType='Resource')">
			<tr>
				<td>
					<xsl:call-template name="resourceDetailLink">
						<xsl:with-param name="namespace" select="'http://www.w3.org/1999/02/22-rdf-syntax-ns#'"/>
						<xsl:with-param name="localname" select="'type'"/>
					</xsl:call-template>
				</td>
				<td>
					<xsl:call-template name="autocomplete">
						<xsl:with-param name="domainTypes" select="$resourceArray"/>
						<xsl:with-param name="propertyURI" select="'http://www.w3.org/1999/02/22-rdf-syntax-ns#type'"/>
						<xsl:with-param name="valueURI" select="concat(namespace-uri(),local-name())"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:if>
		<!-- rdf:type properties -->
		<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
			<tr>
				<td>
					<xsl:call-template name="resourceDetailLink">
						<xsl:with-param name="namespace" select="'http://www.w3.org/1999/02/22-rdf-syntax-ns#'"/>
						<xsl:with-param name="localname" select="'type'"/>
					</xsl:call-template>
				</td>
				<td>
					<xsl:call-template name="property-objects">
						<xsl:with-param name="domainTypes" select="$resourceArray"/>
					</xsl:call-template>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="attributes">
		<xsl:for-each select="@*[not(namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#') and 
													not(local-name='about' or local-name='ID' or local-name='type')]">
			<xsl:sort select="local-name()" order="ascending"/>
			<tr>
				<td>
					<xsl:call-template name="resourceDetailLink">
						<xsl:with-param name="namespace" select="namespace-uri()"/>
						<xsl:with-param name="localname" select="local-name()"/>
					</xsl:call-template>
				</td>
				<td>					
					<input type="text" class="literal" name="{namespace-uri()}{local-name()}">
						<xsl:attribute name="value">
							<xsl:value-of disable-output-escaping="yes" select="."/>
						</xsl:attribute>
					</input>
					<select name="lang">
						<option>
							<xsl:if test="@xml:lang">
								<xsl:attribute name="value">
									<xsl:value-of select="@xml:lang"/>
								</xsl:attribute>
								<xsl:value-of select="@xml:lang"/>
							</xsl:if>
						</option>
						<xsl:copy-of select="$defaultLangs"/>
					</select>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="property-objects">
		<xsl:param name="domainTypes"/>
		<xsl:choose>
			<xsl:when test="@rdf:resource">
				<xsl:call-template name="autocomplete">
					<xsl:with-param name="domainTypes" select="$domainTypes"/>
					<xsl:with-param name="propertyURI" select="concat(namespace-uri(),local-name())"/>
					<xsl:with-param name="valueURI" select="@rdf:resource"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Collection'">
				<xsl:for-each select="child::*[1]">
					<xsl:call-template name="buildList">
						<xsl:with-param name="parentResourceArray" select="$domainTypes"/>
					</xsl:call-template>
				</xsl:for-each>
			</xsl:when>
			<xsl:when test="./*/@rdf:about">
				<xsl:call-template name="autocomplete">
					<xsl:with-param name="domainTypes" select="$domainTypes"/>
					<xsl:with-param name="propertyURI" select="concat(namespace-uri(),local-name())"/>
					<xsl:with-param name="valueURI" select="./*/@rdf:about"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:when test="./*/@rdf:ID">
				<xsl:call-template name="autocomplete">
					<xsl:with-param name="domainTypes" select="$domainTypes"/>
					<xsl:with-param name="propertyURI" select="concat(namespace-uri(),local-name())"/>
					<xsl:with-param name="valueURI" select="./*/@rdf:ID"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="@rdf:parseType='Resource'">
						<input type="hidden" class="object" name="{namespace-uri()}{local-name()}" value="_:{generate-id(.)}"/>
						<xsl:call-template name="embeddedRdfDescription">
							<xsl:with-param name="parentResourceArray" select="$domainTypes"/>
						</xsl:call-template>
					</xsl:when>
					<xsl:otherwise>
						<xsl:variable name="property">
							<xsl:value-of select="namespace-uri()"/>
							<xsl:value-of select="local-name()"/>
						</xsl:variable>
						<xsl:for-each select="child::*">
							<input type="hidden" class="object" name="{$property}" value="_:{generate-id(.)}"/>
							<xsl:call-template name="embeddedRdfDescription">
								<xsl:with-param name="parentResourceArray" select="$domainTypes"/>
							</xsl:call-template>
						</xsl:for-each>
					</xsl:otherwise>
				</xsl:choose>
				<!-- Now, we return to describe the previous resource so get its ID in order to change context -->
				<xsl:choose>
					<xsl:when test="parent::*[@rdf:ID|@rdf:about|@rdf:aboutEach|@rdf:aboutEachPrefix|@rdf:bagID]">
						<!--xsl:variable name="identifier">
							<xsl:value-of select="../@rdf:about|../@rdf:ID|../@rdf:aboutEach|../@rdf:aboutEachPrefix|../@rdf:bagID"/>
						</xsl:variable-->
						<!-- Return to describe base resource but may be ID has been changed, mark this with empty id -->
						<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" value=""/>
					</xsl:when>
					<xsl:otherwise>
						<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" value="_:{generate-id(..)}"/>
					</xsl:otherwise>
				</xsl:choose>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="buildList">
	<xsl:param name="parentResourceArray"/>
		<div class="list-object">
			<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#about" value="_:{generate-id(.)}"/>
			<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#first" value=""/>
			<xsl:call-template name="embeddedRdfDescription">
				<xsl:with-param name="parentResourceArray" select="$parentResourceArray"/>
			</xsl:call-template>
		</div>
       	<div class="list-object">
       		<xsl:choose>
       			<xsl:when test="count(following-sibling::*)>0">
		        	<xsl:call-template name="connector">
						<xsl:with-param name="criteria" select="following-sibling::*"/>
					</xsl:call-template>
       				<xsl:for-each select="following-sibling::*[1]">
       					<input type="hidden" name="http://www.w3.org/1999/02/22-rdf-syntax-ns#rest" value="_:{generate-id(.)}"/>
       					<xsl:call-template name="buildList"/>
       				</xsl:for-each>
       			</xsl:when>
       		</xsl:choose>
       	</div>
	</xsl:template>
		
	<xsl:template name="properties">
		<xsl:param name="domainTypes"/>
		<xsl:for-each select="*[not(namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type')]"> <!--and not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#' and local-name()='label')-->
			<xsl:sort select="local-name()" order="ascending"/>
			<xsl:choose>
				<xsl:when test="text() and count(descendant::*)=0 and not(@rdf:parseType='Resource')">
					<tr>
						<td>
							<xsl:call-template name="resourceDetailLink">
								<xsl:with-param name="namespace" select="namespace-uri()"/>
								<xsl:with-param name="localname" select="local-name()"/>
							</xsl:call-template>
						</td>
						<td>
							<input type="text" class="literal" name="{namespace-uri()}{local-name()}">
								<xsl:attribute name="value">
									<xsl:value-of disable-output-escaping="yes" select="."/>
								</xsl:attribute>
							</input>
							<select name="lang">
								<option>
									<xsl:if test="@xml:lang">
										<xsl:attribute name="value">
											<xsl:value-of select="@xml:lang"/>
										</xsl:attribute>
										<xsl:value-of select="@xml:lang"/>
									</xsl:if>
								</option>
								<xsl:copy-of select="$defaultLangs"/>
							</select>
						</td>
					</tr>
				</xsl:when>
				<xsl:otherwise>
					<tr>
						<td>
							<xsl:call-template name="resourceDetailLink">
								<xsl:with-param name="namespace" select="namespace-uri()"/>
								<xsl:with-param name="localname" select="local-name()"/>
							</xsl:call-template>
						</td>
						<td>
							<xsl:call-template name="property-objects">
								<xsl:with-param name="domainTypes" select="$domainTypes"/>
							</xsl:call-template>
						</td>
					</tr>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="autocomplete">
		<xsl:param name="domainTypes"/>
		<xsl:param name="propertyURI"/>
		<xsl:param name="valueURI"/>
		<xsl:variable name="valueLabel">
			<xsl:call-template name="getLabel">
				<xsl:with-param name="uri" select="$valueURI"/>
			</xsl:call-template>
		</xsl:variable>
		<div id="{generate-id(.)}" >
			<input type="text" value="{$valueLabel}" title="{$valueURI}"/>
			<input type="hidden" class="object" name="{$propertyURI}" 
				value="{$valueURI}"/>
			<div></div>
		</div>
		<script type="text/javascript">
			<xsl:text disable-output-escaping="yes"><![CDATA[ //<![CDATA[  ]]></xsl:text>
				rhizomik.SemanticForms.propertyValueAutocomplete('<xsl:value-of select="generate-id(.)"/>',
					<xsl:value-of select="$domainTypes"/>, '<xsl:value-of select="$propertyURI"/>');
			<xsl:text disable-output-escaping="yes"><![CDATA[//]]]]><![CDATA[>]]></xsl:text>
		</script>
	</xsl:template>

	<xsl:template name="connector">
		<xsl:param name="criteria"/>
		<xsl:choose>
			<xsl:when test="count($criteria)>1">
				<div class="connector" xmlns="http://www.w3.org/1999/xhtml"><xsl:text>,</xsl:text></div>
			</xsl:when>
			<xsl:when test="count($criteria)=1">
				<div class="connector" xmlns="http://www.w3.org/1999/xhtml"><xsl:text> and</xsl:text></div>
			</xsl:when>
		</xsl:choose>	
	</xsl:template>
	
	<xsl:template name="isPreferredLanguage">
		<xsl:variable name="element">
			<xsl:value-of select="name()"/>
		</xsl:variable>
		<xsl:choose>
			<!-- Firstly, select if is preferred language -->
			<xsl:when test="contains(@xml:lang,$language)">
				<xsl:value-of select="true()"/>
			</xsl:when>
			<!-- Secondly, select default language version if there is not a preferred language version -->
			<xsl:when test="contains(@xml:lang,'en') and count(parent::*/*[name()=$element and contains(@xml:lang,$language)])=0">
				<xsl:value-of select="true()"/>
			</xsl:when>
			<!-- Thirdly, select version without language tag if there is not preferred language nor default language version -->
			<xsl:when test="not(@xml:lang) and count(parent::*/*[name()=$element and contains(@xml:lang,$language)])=0 and 
									   count(parent::*/*[name()=$element and contains(@xml:lang,'en')])=0 ">
				<xsl:value-of select="true()"/>
			</xsl:when>
			<!-- Otherwise, ignore -->
			<xsl:otherwise><xsl:value-of select="false()"/></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="getLabel">
		<xsl:param name="uri"/>
		<xsl:choose>
			<xsl:when test="//*[@rdf:about=$uri]/rdfs:label[contains(@xml:lang,$language)]">
				<xsl:value-of select="//*[@rdf:about=$uri]/rdfs:label[contains(@xml:lang,$language)]"/>
			</xsl:when>
			<xsl:when test="//*[@rdf:about=$uri]/rdfs:label[contains(@xml:lang,'en')]">
				<xsl:value-of select="//*[@rdf:about=$uri]/rdfs:label[contains(@xml:lang,'en')]"/>
			</xsl:when>
			<xsl:when test="//*[@rdf:about=$uri]/rdfs:label">
				<xsl:value-of select="//*[@rdf:about=$uri]/rdfs:label"/>
			</xsl:when>
			<xsl:when test="contains($uri,'#')">
				<xsl:value-of select="substring-after($uri,'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="substring-after-last">
				  <xsl:with-param name="text" select="$uri"/>
				  <xsl:with-param name="chars" select="'/'"/>
				</xsl:call-template>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Create a browsable link if rdf:about or rdf:ID property.
		 Otherwise, create a link to describe the property value URI -->
	<xsl:template name="resourceDetailLink">
		<xsl:param name="property"/>
		<xsl:param name="namespace"/>
		<xsl:param name="localname"/>
		<xsl:variable name="uri">
			<xsl:value-of select="concat($namespace,$localname)"/>
		</xsl:variable>
		<xsl:variable name="linkName">
			<xsl:call-template name="getLabel">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="escaped-uri">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$uri"/>
				<xsl:with-param name="replace" select="'#'"/>
				<xsl:with-param name="with" select="'%23'"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="linkTextPrePre">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$linkName"/>
				<xsl:with-param name="replace" select="'_'"/>
				<xsl:with-param name="with" select="' '"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="linkTextPre">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$linkTextPrePre"/>
				<xsl:with-param name="replace" select="'/'"/>
				<xsl:with-param name="with" select="'/ '"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="linkText">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$linkTextPre"/>
				<xsl:with-param name="replace" select="'/ / '"/>
				<xsl:with-param name="with" select="'//'"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="contains($property, 'about')">
				<a class="browse" href="{$uri}"	title="Browse {$uri}">
					<xsl:value-of disable-output-escaping="yes" select="$linkText"/>
				</a>
			</xsl:when>
			<xsl:otherwise>
				<a class="describe" href="?query=DESCRIBE%20&lt;{$escaped-uri}&gt;"
					onClick="javascript:rhz.describeResource('{$uri}'); return false;"
					title="Describe {$uri}">
					<xsl:value-of disable-output-escaping="yes" select="$linkText"/>
				</a>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Create a browsable link if property value is a URL. 
		 Otherwise, write the property value  -->
	<xsl:template name="textDetailLink">
		<xsl:param name="property"/>
		<xsl:param name="value"/>
		<xsl:choose>
			<xsl:when test="contains($value, '://')">
				<xsl:variable name="linkTextPre">
					<xsl:call-template name="replace-string">
						<xsl:with-param name="text" select="$value"/>
						<xsl:with-param name="replace" select="'/'"/>
						<xsl:with-param name="with" select="'/ '"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:variable name="linkText">
					<xsl:call-template name="replace-string">
						<xsl:with-param name="text" select="$linkTextPre"/>
						<xsl:with-param name="replace" select="'/ / '"/>
						<xsl:with-param name="with" select="'//'"/>
					</xsl:call-template>
				</xsl:variable>
				<a class="browse" href="{$value}" title="Browse {$value}">
					<xsl:value-of disable-output-escaping="yes" select="$linkText"/>
				</a>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of disable-output-escaping="yes" select="."/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="substring-after-last">
		<xsl:param name="text"/>
		<xsl:param name="chars"/>
		<xsl:choose>
		  <xsl:when test="contains($text, $chars)">
			<xsl:variable name="last" select="substring-after($text, $chars)"/>
			<xsl:choose>
			  <xsl:when test="contains($last, $chars)">
				<xsl:call-template name="substring-after-last">
				  <xsl:with-param name="text" select="$last"/>
				  <xsl:with-param name="chars" select="$chars"/>
				</xsl:call-template>
			  </xsl:when>
			  <xsl:otherwise>
				<xsl:value-of select="$last"/>
			  </xsl:otherwise>
			</xsl:choose>
		  </xsl:when>
		  <xsl:otherwise>
			<xsl:value-of select="$text"/>
		  </xsl:otherwise>
		</xsl:choose>
  </xsl:template>
  
  <xsl:template name="replace-string">
		<xsl:param name="text"/>
		<xsl:param name="replace"/>
		<xsl:param name="with"/>
		<xsl:choose>
			<xsl:when test="contains($text,$replace)">
				<xsl:value-of select="substring-before($text,$replace)"/>
				<xsl:value-of select="$with"/>
				<xsl:call-template name="replace-string">
					<xsl:with-param name="text" select="substring-after($text,$replace)"/>
					<xsl:with-param name="replace" select="$replace"/>
					<xsl:with-param name="with" select="$with"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="$text"/>
			</xsl:otherwise>
		</xsl:choose>
  </xsl:template>

</xsl:stylesheet>
