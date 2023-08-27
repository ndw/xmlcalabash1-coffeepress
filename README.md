# CoffeePress, an Invisible XML step

This is an an XML Calabash 1.x extension step that implements an Invisible XML processor.

```
<p:declare-step type="cx:invisible-xml">
  <p:input port="grammar" sequence="true"/>
  <p:input port="source" primary="true"/>
  <p:input port="parameters" kind="parameter"/>
  <p:output port="result"/>
  <p:option name="fail-on-error" select="'true'"/>
</p:declare-step>
```

If a `grammar` is not provided, the Invisible XML specification
grammar is used. (In other words a parser without a grammar parses
Invisible XML documents.)

If the `grammar` is provided and it is and XML document with a root
element of `<ixml>`, it is assumed to be the XML version of an
Invisible XML grammar. Any other input is assumed to be a wrapper
around a grammar in the Invisible XML text format; the parser is
constructed from the string value of that input.

The string value of the `source` input is parsed.

The `result` is the XML document that results from the parse. If the
result is ambiguous, one of the parses is returned. If the input
cannot be parsed, then if `fail-on-error` is true, an exception is
thrown. If `fail-on-error` is false, an XML representation of the
failed parse appears on `result`.

The parameter can be any of the parser options described in the
[CoffeeSacks](https://docs.nineml.org/3.2.3/coffeesacks/bk03ch05.html) documentation,
except that the `choose-alternative` function is not supported at this time.
