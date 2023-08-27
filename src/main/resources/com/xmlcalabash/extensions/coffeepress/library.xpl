<p:library xmlns:p="http://www.w3.org/ns/xproc"
           xmlns:cx="http://xmlcalabash.com/ns/extensions"
           version="1.0">

<p:declare-step type="cx:invisible-xml">
  <p:input port="grammar" sequence="true"/>
  <p:input port="source" primary="true"/>
  <p:input port="parameters" kind="parameter"/>
  <p:output port="result"/>
  <p:option name="fail-on-error" select="'true'"/>
</p:declare-step>

</p:library>
