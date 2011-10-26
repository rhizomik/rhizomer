<?xml version="1.0"?>
<!-- 
    Style sheet to transform RDF descriptions to HTML
    Author: http://rhizomik.net/~roberto

	This work is licensed under the Creative Commons Attribution-ShareAlike License.
	To view a copy of this license, visit http://creativecommons.org/licenses/by-sa/3.0/
	or send a letter to Creative Commons, 559 Nathan Abbott Way, Stanford, California 94305, USA.
	License: http://rhizomik.net/redefer/rdf2html.xsl.rdf
-->

<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform" 
	xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#"
	xmlns:rdfs="http://www.w3.org/2000/01/rdf-schema#"
	xmlns:owl="http://www.w3.org/2002/07/owl#">
	
	<xsl:output media-type="text/xhtml" encoding="UTF-8" indent="yes" omit-xml-declaration="no" method="html"/>
	
	<xsl:strip-space elements="*"/>

	<xsl:param name="base"></xsl:param>
	<xsl:param name="language">en</xsl:param>
	<xsl:param name="mode">rhizomer</xsl:param>
	<xsl:param name="namespaces">false</xsl:param>
	<xsl:param name="logo">false</xsl:param>
	<xsl:param name="coNS">http://rhizomik.net/ontologies/2009/09/copyrightonto.owl#</xsl:param>
 	<xsl:param name="grNS">http://purl.org/goodrelations/v1#</xsl:param>

	
	<xsl:template match="/">
		<xsl:if test="$mode='html'">
			<html version="XHTML+RDFa 1.0" xmlns="http://www.w3.org/1999/xhtml">
				<head>
					<meta http-equiv="Content-Type" content="text/xhtml; charset=UTF-8"/>
					<title>Rhizomik - ReDeFer - License2Text</title>
					<link href="{$base}/license2text.css" type="text/css" rel="stylesheet" />
				</head>
				<body>
					<xsl:apply-templates select="rdf:RDF"/>
					<xsl:if test="$logo='true'">
						<div id="footlogo">
							<div id="logo">
								<a href="http://rhizomik.net"  xmlns="http://www.w3.org/1999/xhtml">
									<img src="http://rhizomik.net/images/rhizomer.small.png" alt="Rhizomik"/> Powered by Rhizomik
								</a>
							</div>
						</div>
					</xsl:if>
				</body>
			</html>
		</xsl:if>
		<xsl:if test="$mode='snippet' or $mode='rhizomer'">
			<link href="{$base}license2text.css" type="text/css" rel="stylesheet" />
			<xsl:apply-templates select="rdf:RDF"/>
			<xsl:if test="$logo='true'">
				<div id="footlogo">
					<div id="logo">
						<a href="http://rhizomik.net"  xmlns="http://www.w3.org/1999/xhtml">
							<img src="http://rhizomik.net/images/rhizomer.small.png" alt="Rhizomik"/> Powered by Rhizomik
						</a>
					</div>
				</div>
			</xsl:if>
		</xsl:if>
		<!-- Show error message if we have a parsererror -->
		<xsl:value-of select="//*[local-name()='sourcetext']"/>
	</xsl:template>
	
	<xsl:template match="rdf:RDF">
		<div class="license2text" xmlns="http://www.w3.org/1999/xhtml">
			<!-- Generate the xmlns for RDFa from those in the RDF/XML and attach to div#rdf2html -->
			<xsl:variable name="namespaces">
				<xsl:for-each select="/rdf:RDF/namespace::*[name()!='xml' and name()!='xsd']">
					<xsl:choose>
						<!-- The base NS in the output is XHTML so keep base NS in input RDF file with alias "base" -->
						<xsl:when test="name()=''">
							<xsl:element name="base:dummy-for-xmlns" namespace="{.}"/>
						</xsl:when>
						<xsl:otherwise>
							<xsl:element name="{concat(name(),':dummy-for-xmlns')}" namespace="{.}"/>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:for-each>
				<xsl:element name="xsd:dummy-for-xmlns" namespace="http://www.w3.org/2001/XMLSchema#"/>
			</xsl:variable>
			<xsl:copy-of select="$namespaces/*/namespace::*"/>

			<!-- If no RDF descriptions... -->
			<xsl:if test="count(child::*)=0">
				<p xmlns="http://www.w3.org/1999/xhtml">No data retrieved.</p>
			</xsl:if>
			<!-- If rdf:RDF has child elements, they are descriptions... -->
			<xsl:for-each select="child::*">
				<xsl:sort select="@rdf:about" order="ascending"/>
				<xsl:call-template name="rdfDescription"/>
			</xsl:for-each>
		</div>
	</xsl:template>
  	
	<xsl:template name="rdfDescription">
		<xsl:choose>
			<!-- RDF Description that contains more than labels (e.g. just type instead of rdf:Description) or is the only description -->
			<xsl:when test="(count(following-sibling::*)=0 and count(preceding-sibling::*)=0) or not(local-name()='Description') or
							*[not(name()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label')] | 
							@*[not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label' or local-name()='about')]">
				<div class="description" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:if test="@rdf:ID|@rdf:about">
						<xsl:attribute name="about" >
							<xsl:value-of select="@rdf:ID|@rdf:about"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:call-template name="header"/>
					<xsl:call-template name="action"/>
				</div>
			</xsl:when>
			<xsl:otherwise><!-- Ignore RDF Descriptions with just labels --></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="embeddedRdfDescription">
		<xsl:choose>
			<xsl:when test="namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and 
							(local-name()='Seq' or local-name()='Alt' or local-name()='Bag')">
				<div>
					<xsl:attribute name="typeof">
						<xsl:call-template name="curie">
							<xsl:with-param name="uri" select="concat(namespace-uri(),local-name())"/>
						</xsl:call-template>
					</xsl:attribute>
					<xsl:call-template name="collectionItems"/>
				</div>
			</xsl:when>
			<!-- Embedded RDF Description that contains more than labels -->
			<xsl:when test="*[not(name()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label')] |
							@*[not(namespace-uri()='http://www.w3.org/2000/01/rdf-schema#') and not(local-name()='label' or local-name()='about')]">
				<div xmlns="http://www.w3.org/1999/xhtml">
					<xsl:if test="not(@rdf:parseType='Resource')">
						<xsl:call-template name="header"/>
					</xsl:if>
					<xsl:if test="@rdf:parseType='Resource'">
						<xsl:attribute name="typeof">
							<xsl:value-of select="'rdfs:Resource'"/>
						</xsl:attribute>
					</xsl:if>
					<xsl:call-template name="attributes"/>
					<xsl:call-template name="action"/>
				</div>
			</xsl:when>
			<!-- Embeded RDF Description with just labels, just take resource -->
			<xsl:when test="parent::*[not(name()='rdf:RDF')]">
				<xsl:call-template name="resourceDetailLink">
					<xsl:with-param name="property" select="local-name(parent::*)"/>
					<xsl:with-param name="namespace" select="''"/>
					<xsl:with-param name="localname" select="@rdf:about"/>
				</xsl:call-template>
			</xsl:when>
			<xsl:otherwise><!-- Ignore other --></xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<!-- Build the description header with the resource identifier and types, if available -->
	<xsl:template name="header">
		<xsl:choose>
			<xsl:when test="@rdf:ID|@rdf:about or not(local-name()='Description') or 
								count(*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type'])>0">
				<xsl:if test="@rdf:ID|@rdf:about">
					<xsl:attribute name="about" >
						<xsl:value-of select="@rdf:ID|@rdf:about"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:if test="not(local-name()='Description')">
					<xsl:attribute name="typeof">
						<xsl:call-template name="curie">
							<xsl:with-param name="uri" select="concat(namespace-uri(),local-name())"/>
						</xsl:call-template>
					</xsl:attribute>
				</xsl:if>
			</xsl:when>
			<xsl:otherwise>
				<xsl:attribute name="typeof">
					<xsl:value-of select="'rdfs:Resource'"/>
				</xsl:attribute>
			</xsl:otherwise>
		</xsl:choose>
		<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
			<span rel="rdf:type" resource="{@rdf:resource}" xmlns="http://www.w3.org/1999/xhtml"/>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="attributes">
		<xsl:for-each select="@*[not(namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#')]">
								<!-- and not(local-name='about' or local-name='ID' or local-name='type')]" -->
			<xsl:sort select="local-name()" order="ascending"/>
			<tr xmlns="http://www.w3.org/1999/xhtml"> 
				<td xmlns="http://www.w3.org/1999/xhtml">
					<xsl:call-template name="resourceDetailLink">
						<xsl:with-param name="property" select="''"/>
						<xsl:with-param name="namespace" select="namespace-uri()"/>
						<xsl:with-param name="localname" select="local-name()"/>
					</xsl:call-template>
				</td>
				<td property="{name()}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:value-of select="."/>
				</td>
			</tr>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="property-objects">
		<xsl:choose>
			<xsl:when test="@rdf:resource">
				<xsl:call-template name="rdf_resource-attribute"/>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Resource'">
				<xsl:call-template name="embeddedRdfDescription"/>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Collection'">
				<xsl:for-each select="child::*[1]">
					<xsl:call-template name="buildList"/>
				</xsl:for-each>
			</xsl:when>
			<xsl:when test="@rdf:parseType='Literal'">
				<xsl:attribute name="datatype">
					<xsl:call-template name="curie">
						<xsl:with-param name="uri" select="'http://www.w3.org/1999/02/22-rdf-syntax-ns#XMLLiteral'"/>
					</xsl:call-template>
				</xsl:attribute>
				<xsl:if test="@xml:lang">
					<xsl:attribute name="xml:lang">
						<xsl:value-of select="@xml:lang"/>
					</xsl:attribute>
				</xsl:if>
				<xsl:apply-templates mode="copy-subtree"/>
			</xsl:when>
			<xsl:when test="child::*">
				<xsl:for-each select="child::*">
					<xsl:call-template name="embeddedRdfDescription"/>
				</xsl:for-each>
			</xsl:when>
			<xsl:otherwise>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="buildList">
		<div class="property-object" typeof="rdf:List">
			<div class="property-object" rel="rdf:first">
				<xsl:call-template name="embeddedRdfDescription"/>
			</div>
        	<div class="property-object" rel="rdf:rest">
        		<xsl:choose>
        			<xsl:when test="count(following-sibling::*)>0">
			        	<xsl:call-template name="connector">
							<xsl:with-param name="criteria" select="following-sibling::*"/>
						</xsl:call-template>
        				<xsl:for-each select="following-sibling::*[1]">
        					<xsl:call-template name="buildList"/>
        				</xsl:for-each>
        			</xsl:when>
        			<xsl:otherwise>
        				<xsl:attribute name="resource"><xsl:value-of select="'[rdf:nil]'"/></xsl:attribute>
        			</xsl:otherwise>
        		</xsl:choose>
        	</div>
        </div>
	</xsl:template>
	
	<xsl:template name="rdf_resource-attribute">
		<xsl:if test="@rdf:resource">
			<xsl:call-template name="resourceDetailLink">
				<xsl:with-param name="property" select="local-name()"/>
				<xsl:with-param name="namespace" select="''"/>
				<xsl:with-param name="localname" select="@rdf:resource"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="property-attributes">
		<xsl:if test="@xml:lang">
			<xsl:attribute name="xml:lang">
				<xsl:value-of select="@xml:lang"/>
			</xsl:attribute>
		</xsl:if>
		<xsl:if test="@rdf:datatype">
			<xsl:attribute name="datatype">
				<xsl:call-template name="curie">
					<xsl:with-param name="uri" select="@rdf:datatype"/>
				</xsl:call-template>
			</xsl:attribute>
			<xsl:variable name="datatype-xmlns">
				<xsl:call-template name="xmlns">
					<xsl:with-param name="uri" select="@rdf:datatype"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:copy-of select="$datatype-xmlns/*/namespace::*"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="action">
		<xsl:choose>
			<xsl:when test="local-name()='Agree' and namespace-uri()=$coNS">
				<xsl:call-template name="action-agree"/>
			</xsl:when>
			<xsl:when test="(local-name()='Attribute' and namespace-uri()=$coNS) or
							rdfs:subClassOf[@rdf:resource=concat($coNS,'Attribute')] or
							rdfs:subClassOf/*[@rdf:about=concat($coNS,'Attribute')]">
				<xsl:call-template name="action-attribute"/>
			</xsl:when>
			<xsl:when test="(local-name()='PriceSpecification' and namespace-uri()=$grNS) or
							rdfs:subClassOf[@rdf:resource=concat($grNS,'PriceSpecification')] or
							rdfs:subClassOf/*[@rdf:about=concat($grNS,'PriceSpecification')]">
				<xsl:call-template name="priceSpecification"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:call-template name="caserole-agent"/>
				<xsl:call-template name="verb"></xsl:call-template>
				<xsl:call-template name="caserole-theme"/>
				<xsl:call-template name="caserole-result"/>
				<xsl:call-template name="caserole-condition"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template name="action-agree">
		<xsl:call-template name="caserole-agent"/>
		<xsl:call-template name="verb"></xsl:call-template>
		<div class="connector"><xsl:text>that</xsl:text></div>
		<xsl:call-template name="caserole-theme"/>
		<xsl:call-template name="caserole-condition"/>
	</xsl:template>
	
	<xsl:template name="action-attribute">
		<xsl:if test="*[local-name()='agent' and namespace-uri()=$coNS] or
					  */owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'agent')]]">
			<xsl:call-template name="caserole-agent"/>
		</xsl:if>
		<xsl:call-template name="verb"></xsl:call-template>
		<xsl:call-template name="caserole-recipient"/>
		<xsl:if test="*[local-name()='theme' and namespace-uri()=$coNS] or
					  */owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'theme')]]">
			<div class="connector"><xsl:text>on</xsl:text></div>
			<xsl:call-template name="caserole-theme"/>
		</xsl:if>
	</xsl:template>
	
	<xsl:template name="priceSpecification">
		<xsl:call-template name="verb"></xsl:call-template>
		<div class="connector"><xsl:text>of</xsl:text></div>
		<xsl:call-template name="amount"/>
		<xsl:call-template name="currency"/>
	</xsl:template>
	
	<xsl:template name="amount">
		<xsl:for-each select="*[local-name()='hasCurrencyValue' and namespace-uri()=$grNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($grNS,'hasCurrencyValue')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="currency">
		<xsl:for-each select="*[local-name()='hasCurrency' and namespace-uri()=$grNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($grNS,'hasCurrency')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Agent -->
	<xsl:template name="caserole-agent">
		<xsl:if test="not(*[local-name()='agent' and namespace-uri()=$coNS] or
					  */owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'agent')]])">
			<div class="connector" xmlns="http://www.w3.org/1999/xhtml"><xsl:text>anyone</xsl:text></div>
		</xsl:if>
		<xsl:for-each select="*[local-name()='agent' and namespace-uri()=$coNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'agent')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Theme -->
	<xsl:template name="caserole-theme">
		<xsl:for-each select="*[local-name()='theme' and namespace-uri()=$coNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[descendant::*[@rdf:resource=concat($coNS,'theme') or 
															  @rdf:about=concat($coNS,'theme')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Recipient -->
	<xsl:template name="caserole-recipient">
		<xsl:for-each select="*[local-name()='recipient' and namespace-uri()=$coNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[descendant::*[@rdf:resource=concat($coNS,'recipient') or 
															  @rdf:about=concat($coNS,'recipient')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Condition -->	
	<xsl:template name="caserole-condition">
		<xsl:if test="*[local-name()='condition' and namespace-uri()=$coNS]">
			<div class="connector"><xsl:text>if</xsl:text></div>
		</xsl:if>
		<xsl:for-each select="*[local-name()='condition' and namespace-uri()=$coNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'condition')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<!-- Result -->
	<xsl:template name="caserole-result">
		<xsl:if test="*[local-name()='result' and namespace-uri()=$coNS] or
					  	*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'result')]]">
			<div class="connector"><xsl:text>resulting in</xsl:text></div>
		</xsl:if>
		<xsl:for-each select="*[local-name()='result' and namespace-uri()=$coNS]">
			<xsl:call-template name="property"></xsl:call-template>
		</xsl:for-each>
		<xsl:for-each select="*/owl:Restriction[child::owl:onProperty[@rdf:resource=concat($coNS,'result')]]">
			<xsl:call-template name="restriction"></xsl:call-template>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="restriction">
		<xsl:variable name="propertyCurie">
			<xsl:call-template name="curie">
				<xsl:with-param name="uri" select="child::onProperty/@rdf:resource"/>
			</xsl:call-template>
		</xsl:variable>
		<div typeof="owl:Restriction">
			<div class="property-object" rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
				<xsl:for-each select="*[local-name()='hasValue']">
					<xsl:call-template name="property"/>
				</xsl:for-each>
				<xsl:for-each select="*[local-name()='someValuesFrom']">
					<div class="connector"><xsl:text>some </xsl:text></div>
					<xsl:call-template name="property"/>
				</xsl:for-each>
				<xsl:for-each select="*[local-name()='allValuesFrom']">
					<div class="connector"><xsl:text>all </xsl:text></div>
					<xsl:call-template name="property"/>
				</xsl:for-each>
			</div>
		</div>
	</xsl:template>
	
	<xsl:template name="verb">
		<div class="verb" xmlns="http://www.w3.org/1999/xhtml">
			<xsl:if test="not(local-name()='Description') and namespace-uri()=$coNS">
				<xsl:call-template name="resourceDetailLink">
					<xsl:with-param name="property" select="'type'"/>
					<xsl:with-param name="namespace" select="namespace-uri()"/>
					<xsl:with-param name="localname" select="local-name()"/>
				</xsl:call-template>
				<xsl:call-template name="verb-thirdperson"/>
				<xsl:if test="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
					<xsl:text>, </xsl:text>
				</xsl:if>
			</xsl:if>
			<!-- rdf:type properties -->
			<xsl:for-each select="*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
				<div rel="rdf:type" class="connector" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:call-template name="property-objects"/>
				<xsl:call-template name="verb-thirdperson"/>
				</div>
				<xsl:if test="following-sibling::*[namespace-uri()='http://www.w3.org/1999/02/22-rdf-syntax-ns#' and local-name()='type']">
					<div class="connector"><xsl:text>, </xsl:text></div>
				</xsl:if>
			</xsl:for-each>
			<!-- rdfs:subClassOf -->
			<xsl:for-each select="rdfs:subClassOf[@rdf:resource]">
				<xsl:call-template name="property-objects"/>
				<xsl:call-template name="verb-thirdperson"/>
			</xsl:for-each>
			<xsl:for-each select="rdfs:subClassOf/rdf:Description">
				<xsl:call-template name="resourceDetailLink">
					<xsl:with-param name="property" select="'type'"/>
					<xsl:with-param name="namespace" select="''"/>
					<xsl:with-param name="localname" select="@rdf:about"/>
				</xsl:call-template>
				<xsl:call-template name="verb-thirdperson"/>
			</xsl:for-each>
			<xsl:for-each select="rdfs:subClassOf/owl:Class/owl:unionOf/*">
				<xsl:call-template name="embeddedRdfDescription"/>
				<xsl:call-template name="verb-thirdperson"/>
				<xsl:call-template name="connector-or">
					<xsl:with-param name="criteria" select="following-sibling::*"/>
				</xsl:call-template>
			</xsl:for-each>
			<xsl:for-each select="rdfs:subClassOf/owl:Class/owl:intersectionOf/*">
				<xsl:call-template name="embeddedRdfDescription"/>
				<xsl:call-template name="verb-thirdperson"/>
				<xsl:call-template name="connector">
					<xsl:with-param name="criteria" select="following-sibling::*"/>
				</xsl:call-template>
			</xsl:for-each>
		</div>
	</xsl:template>
	
	<xsl:template name="verb-thirdperson">
		<!-- xsl:if test="count(*[local-name()='agent' and namespace-uri()=$coNS])&lt;2">
			<xsl:text>s</xsl:text>
		</xsl:if -->
	</xsl:template>
	
	<xsl:template name="property">
			<xsl:variable name="property-name">
				<xsl:value-of select="local-name()"></xsl:value-of>
			</xsl:variable>
			<xsl:variable name="property-namespace">
				<xsl:value-of select="namespace-uri()"></xsl:value-of>
			</xsl:variable>
			<xsl:variable name="propertyCurie">
				<xsl:call-template name="curie">
					<xsl:with-param name="uri" select="concat(namespace-uri(),local-name())"/>
				</xsl:call-template>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="text() and count(child::*)=0 and not(@rdf:parseType)">
					<xsl:variable name="isPreferredLanguage">
						<xsl:call-template name="isPreferredLanguage"/>
					</xsl:variable>
					<!-- Generate HTML rendering just for the preferred language -->
					<xsl:choose>
						<xsl:when test="$isPreferredLanguage='true'">
							<xsl:choose>
								<xsl:when test="preceding-sibling::*[local-name()=$property-name and
													namespace-uri()=$property-namespace]">
									<!-- Ignore, already processed with first value of the same property -->
								</xsl:when>
								<xsl:otherwise>
									<div class="{$property-namespace}{$property-name}" xmlns="http://www.w3.org/1999/xhtml">
										<div property="{$propertyCurie}" content="{.}" xmlns="http://www.w3.org/1999/xhtml">
											<xsl:call-template name="property-attributes"></xsl:call-template>
											
											<div class="property-object" xmlns="http://www.w3.org/1999/xhtml">
												<xsl:call-template name="textDetailLink">
													<xsl:with-param name="property" select="local-name()"/>
													<xsl:with-param name="value" select="."/>
												</xsl:call-template>
											</div>
											<xsl:call-template name="connector">
												<xsl:with-param name="criteria" select="following-sibling::*[local-name()=$property-name and
																						namespace-uri()=$property-namespace]"/>
											</xsl:call-template>
											<xsl:for-each select="following-sibling::*[local-name()=$property-name and
																		namespace-uri()=$property-namespace]">
												
												<span property="{$propertyCurie}" content="{.}" xmlns="http://www.w3.org/1999/xhtml">
													<xsl:call-template name="property-attributes"></xsl:call-template>
												</span>
												<div class="property-object" xmlns="http://www.w3.org/1999/xhtml">
													<xsl:call-template name="textDetailLink">
														<xsl:with-param name="property" select="local-name()"/>
														<xsl:with-param name="value" select="."/>
													</xsl:call-template>
												</div>
												<xsl:call-template name="connector">
													<xsl:with-param name="criteria" select="following-sibling::*[local-name()=$property-name and
																							namespace-uri()=$property-namespace]"/>
												</xsl:call-template>
											</xsl:for-each>	
										</div>
									</div>
								</xsl:otherwise>
							</xsl:choose>				
						</xsl:when>
						<xsl:otherwise>
							<!-- Generate RDFa also for non preferred languages -->
							<div property="{$propertyCurie}" content="{.}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:call-template name="property-attributes"></xsl:call-template>
							</div>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="preceding-sibling::*[local-name()=$property-name and
											namespace-uri()=$property-namespace]">
							<!-- Ignore, already processed with first value of the same property -->
						</xsl:when>
						<xsl:otherwise>
							<div class="{$property-namespace}{$property-name}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:choose>
									<xsl:when test="@rdf:parseType='Literal' or 
													(count(child::*)=0 and not(@rdf:resource or @rdf:nodeID or @rdf:parseType='Resource'))">
										<div class="property-object" property="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
											<xsl:call-template name="property-objects"/>
										</div>
									</xsl:when>
									<xsl:otherwise>
										<div class="property-object" rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
											<xsl:call-template name="property-objects"/>
											<xsl:call-template name="connector">
												<xsl:with-param name="criteria" select="following-sibling::*
													[local-name()=$property-name and namespace-uri()=$property-namespace]"/>
											</xsl:call-template>
										</div>
									</xsl:otherwise>
								</xsl:choose>
								<xsl:for-each select="following-sibling::*[local-name()=$property-name and
														namespace-uri()=$property-namespace]">
									<div class="property-object" rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
										<xsl:call-template name="property-objects"/>
										<xsl:call-template name="connector">
											<xsl:with-param name="criteria" select="following-sibling::*
													[local-name()=$property-name and namespace-uri()=$property-namespace]"/>
										</xsl:call-template>
									</div>
								</xsl:for-each>
							</div>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:otherwise>
			</xsl:choose>	
	</xsl:template>
	
	<xsl:template name="collectionItems">
		<xsl:for-each select="*">
			<xsl:variable name="propertyCurie">
				<xsl:value-of select="'rdf:li'"/>
			</xsl:variable>
			<xsl:choose>
				<xsl:when test="text() and count(descendant::*)=0 and not(@rdf:parseType)">
					<xsl:variable name="isPreferredLanguage">
						<xsl:call-template name="isPreferredLanguage"/>
					</xsl:variable>
					<!-- Generate HTML rendering just for the preferred language -->
					<xsl:choose>
						<xsl:when test="$isPreferredLanguage='true'">
							<div property="{$propertyCurie}" content="{.}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:if test="@xml:lang">
									<xsl:attribute name="xml:lang">
										<xsl:value-of select="@xml:lang"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:if test="@rdf:datatype">
									<xsl:attribute name="datatype">
										<xsl:call-template name="curie">
											<xsl:with-param name="uri" select="@rdf:datatype"/>
										</xsl:call-template>
									</xsl:attribute>
									<xsl:variable name="datatype-xmlns">
										<xsl:call-template name="xmlns">
											<xsl:with-param name="uri" select="@rdf:datatype"/>
										</xsl:call-template>
									</xsl:variable>
									<xsl:copy-of select="$datatype-xmlns/*/namespace::*"/>
								</xsl:if>
								<!--xsl:call-template name="property-attributes"/-->
								<xsl:call-template name="textDetailLink">
									<xsl:with-param name="property" select="local-name()"/>
									<xsl:with-param name="value" select="."/>
								</xsl:call-template>
								<xsl:call-template name="connector">
									<xsl:with-param name="criteria" select="following-sibling::*"/>
								</xsl:call-template>
							</div>					
						</xsl:when>
						<xsl:otherwise>
							<!-- Generate RDFa also for non preferred languages -->
							<div property="{$propertyCurie}" content="{.}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:if test="@xml:lang">
									<xsl:attribute name="xml:lang">
										<xsl:value-of select="@xml:lang"/>
									</xsl:attribute>
								</xsl:if>
								<xsl:if test="@rdf:datatype">
									<xsl:attribute name="datatype">
										<xsl:call-template name="curie">
											<xsl:with-param name="uri" select="@rdf:datatype"/>
										</xsl:call-template>
									</xsl:attribute>
									<xsl:variable name="datatype-xmlns">
										<xsl:call-template name="xmlns">
											<xsl:with-param name="uri" select="@rdf:datatype"/>
										</xsl:call-template>
									</xsl:variable>
									<xsl:copy-of select="$datatype-xmlns/*/namespace::*"/>
								</xsl:if>
							</div>
						</xsl:otherwise>
					</xsl:choose>
				</xsl:when>
				<xsl:otherwise>
					<xsl:choose>
						<xsl:when test="@rdf:parseType='Literal'">
							<div class="property-object" property="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:call-template name="property-objects"/>
							</div>
						</xsl:when>
						<xsl:otherwise>
							<div class="property-object" rel="{$propertyCurie}" xmlns="http://www.w3.org/1999/xhtml">
								<xsl:call-template name="property-objects"/>
							</div>
						</xsl:otherwise>
					</xsl:choose>
					<xsl:call-template name="connector">
						<xsl:with-param name="criteria" select="following-sibling::*"/>
					</xsl:call-template>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:for-each>
	</xsl:template>
	
	<xsl:template name="connector">
		<xsl:param name="criteria" />
		<xsl:choose>
			<xsl:when test="count($criteria)>1">
				<div class="connector"><xsl:text>, </xsl:text></div>
			</xsl:when>
			<xsl:when test="count($criteria)=1">
				<div class="connector"><xsl:text> and </xsl:text></div>
			</xsl:when>
		</xsl:choose>	
	</xsl:template>
	
	<xsl:template name="connector-or">
		<xsl:param name="criteria" />
		<xsl:choose>
			<xsl:when test="count($criteria)>1">
				<div class="connector"><xsl:text>, </xsl:text></div>
			</xsl:when>
			<xsl:when test="count($criteria)=1">
				<div class="connector"><xsl:text> or </xsl:text></div>
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
		<xsl:param name="property"/>

		<xsl:variable name="last-char">
			<xsl:value-of select="substring($uri,string-length($uri)-1)"/>
		</xsl:variable>
		<xsl:variable name="uri-noslash">
			<xsl:choose>
				<xsl:when test="contains($last-char,'/')">
                      	<xsl:value-of select="substring($uri,1,string-length($uri)-1)"/>
				</xsl:when>
				<xsl:otherwise>
					<xsl:value-of select="$uri"/>
				</xsl:otherwise>
			</xsl:choose>
		</xsl:variable>

		
		<xsl:variable name="namespace">
			<xsl:call-template name="get-ns">
				<xsl:with-param name="uri" select="$uri-noslash"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="localname">
			<xsl:call-template name="get-name">
				<xsl:with-param name="uri" select="$uri-noslash"/>
			</xsl:call-template>
		</xsl:variable>		
		
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
			<xsl:when test="$namespaces='true' and namespace::*[.=$namespace and name()!='']">
				<xsl:variable name="namespaceAlias">
					<xsl:value-of select="name(namespace::*[.=$namespace and name()!=''])"/>
				</xsl:variable>
				<xsl:value-of select="concat(concat($namespaceAlias,':'),$localname)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:choose>
					<xsl:when test="$namespaces='true' and $namespace = namespace::*[name()='']">
						<xsl:value-of select="concat(':',$localname)"/>
					</xsl:when>
					<xsl:when test="$namespaces='false' and $namespace = namespace::*[name()='']">
						<xsl:value-of select="$localname"/>
					</xsl:when>
					<xsl:when test="$namespaces='true' and $property = 'type'">
						<xsl:value-of select="$localname"/>
					</xsl:when>
					<xsl:when test="$namespaces='false'">
						<xsl:value-of select="$localname"/>
					</xsl:when>
					<xsl:otherwise>
						<xsl:value-of select="$uri"/>
					</xsl:otherwise>
				</xsl:choose>
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
				<xsl:with-param name="property" select="$property"/>
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
			<xsl:choose>
				<xsl:when test="contains($linkTextPrePre, '://')">
					<xsl:call-template name="replace-string">
						<xsl:with-param name="text" select="$linkTextPrePre"/>
						<xsl:with-param name="replace" select="'/'"/>
						<xsl:with-param name="with" select="'/ '"/>
					</xsl:call-template>
				</xsl:when>
				<xsl:otherwise><xsl:value-of select="$linkTextPrePre"/></xsl:otherwise>
			</xsl:choose>
		</xsl:variable>
		<xsl:variable name="linkText">
			<xsl:call-template name="replace-string">
				<xsl:with-param name="text" select="$linkTextPre"/>
				<xsl:with-param name="replace" select="'/ / '"/>
				<xsl:with-param name="with" select="'//'"/>
			</xsl:call-template>
		</xsl:variable>
		<!-- xsl:choose>
			<xsl:when test="contains($linkText, '://')">
				<a class="browse" href="{$uri}"	title="{$uri}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:value-of select="$linkText"/>
				</a>
			</xsl:when>
			<xsl:otherwise -->
				<xsl:choose>
					<xsl:when test="$mode='rhizomer' and $property!='about'">
						<a class="describe" href="?query=DESCRIBE%20&lt;{$escaped-uri}&gt;"
							onclick="javascript:rhz.describeResource('{$uri}'); return false;"
							title="Describe {$uri}" xmlns="http://www.w3.org/1999/xhtml">
							<xsl:if test="$property != ''">
								<xsl:attribute name="resource">
									<xsl:value-of select="$uri"/>
								</xsl:attribute>
							</xsl:if>
							<xsl:value-of select="$linkText"/>
						</a>
					</xsl:when>
					<xsl:otherwise>
						<a class="browse" href="{$uri}" title="Browse {$uri}" xmlns="http://www.w3.org/1999/xhtml">
							<xsl:value-of select="$linkText"/>
						</a>
					</xsl:otherwise>
				</xsl:choose>
			<!-- /xsl:otherwise>
		</xsl:choose -->
	</xsl:template>
	
	<!-- Create a browsable link if property value is a URL. 
		 Otherwise, write the property value  -->
	<xsl:template name="textDetailLink">
		<xsl:param name="property"/>
		<xsl:param name="value"/>
		<xsl:choose>
			<xsl:when test="starts-with($value, 'http://') or starts-with($value, 'https://')">
				<xsl:variable name="linkTextPrePre">
					<xsl:call-template name="replace-string">
						<xsl:with-param name="text" select="$value"/>
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
				<a class="browse" href="{$value}" title="Browse {$value}" xmlns="http://www.w3.org/1999/xhtml">
					<xsl:value-of select="$linkText"/>
				</a>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="."/>
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
  
  	<xsl:template name="substring-before-last">
		<xsl:param name="text"/>
		<xsl:param name="chars"/>
		<xsl:choose>
		  <xsl:when test="contains($text, $chars)">
			<xsl:variable name="before" select="substring-before($text, $chars)"/>
			<xsl:variable name="after" select="substring-after($text, $chars)"/>
			<xsl:choose>
			  <xsl:when test="contains($after, $chars)">
			    <xsl:variable name="before-last">
					<xsl:call-template name="substring-before-last">
				  		<xsl:with-param name="text" select="$after"/>
				  		<xsl:with-param name="chars" select="$chars"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat($before,concat($chars,$before-last))"/>
			  </xsl:when>
			  <xsl:otherwise>
				<xsl:value-of select="$before"/>
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
  
    <xsl:template name="get-ns">
		<xsl:param name="uri"/>
		<xsl:choose>
	  		<xsl:when test="contains($uri,'#')">
				<xsl:value-of select="concat(substring-before($uri,'#'),'#')"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:variable name="ns-without-slash">
					<xsl:call-template name="substring-before-last">
						<xsl:with-param name="text" select="$uri"/>
						<xsl:with-param name="chars" select="'/'"/>
					</xsl:call-template>
				</xsl:variable>
				<xsl:value-of select="concat($ns-without-slash, '/')"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
    <xsl:template name="get-name">
		<xsl:param name="uri"/>
		<xsl:choose>
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
	
	<xsl:template name="xmlns">
		<xsl:param name="uri"/>
		<xsl:variable name="namespace">
			<xsl:call-template name="get-ns">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="namespace::*[.=$namespace]">
			</xsl:when>
			<xsl:otherwise>
				<xsl:element name="{concat(generate-id(),':dummy-for-xmlns')}" namespace="{$namespace}"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
		
	<xsl:template name="curie">
		<xsl:param name="uri"/>
		<xsl:variable name="namespace">
			<xsl:call-template name="get-ns">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:variable name="localname">
			<xsl:call-template name="get-name">
				<xsl:with-param name="uri" select="$uri"/>
			</xsl:call-template>
		</xsl:variable>
		<xsl:choose>
			<xsl:when test="namespace::*[.=$namespace and name()!='']">
				<xsl:variable name="namespaceAlias">
					<xsl:value-of select="name(namespace::*[.=$namespace and name()!=''])"/>
				</xsl:variable>
				<xsl:value-of select="concat(concat($namespaceAlias,':'),$localname)"/>
			</xsl:when>			
			<xsl:when test="$namespace='http://www.w3.org/2001/XMLSchema#'">
				<xsl:value-of select="concat('xsd:',$localname)"/>
			</xsl:when>
			<xsl:when test="namespace::*[name()='']">
				<xsl:value-of select="concat('base:',$localname)"/>
			</xsl:when>
			<xsl:otherwise>
				<xsl:value-of select="concat(concat(generate-id(),':'),$localname)"/>
			</xsl:otherwise>
		</xsl:choose>
	</xsl:template>
	
	<xsl:template match="*|@*|text()" mode="copy-subtree">
		<xsl:copy>
			<xsl:apply-templates select="*|@*|text()" mode="copy-subtree"/>
		</xsl:copy>
	</xsl:template>
	
	<xsl:template match="*|@*|text()"></xsl:template>
	
</xsl:stylesheet>
