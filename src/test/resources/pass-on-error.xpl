<p:declare-step xmlns:p="http://www.w3.org/ns/xproc" version="1.0"
                xmlns:c="http://www.w3.org/ns/xproc-step"
                xmlns:cx="http://xmlcalabash.com/ns/extensions"
                exclude-inline-prefixes="cx"
                name="main">
<p:output port="result"/>
<p:input port="parameters" kind="parameter"/>
<p:serialization port="result" indent="true"/>

<p:import href="http://xmlcalabash.com/extension/steps/invisible-xml.xpl"/>

<cx:invisible-xml>
  <p:input port="grammar">
    <p:data href="sentence.ixml" content-type="text/plain"/>
  </p:input>
  <p:input port="source">
    <p:inline><doc>999</doc></p:inline>
  </p:input>
  <p:with-option name="fail-on-error" select="'false'"/>
</cx:invisible-xml>

</p:declare-step>
