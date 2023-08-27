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
    <p:inline>
      <doc>
text: sentence .
sentence: word++WS .
word: [L]+ .
-WS: -" " .
      </doc>
    </p:inline>
  </p:input>
  <p:input port="source">
    <p:inline><doc>this is a test</doc></p:inline>
  </p:input>
</cx:invisible-xml>

</p:declare-step>
