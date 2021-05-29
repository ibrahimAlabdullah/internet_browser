package nodes;

import Connction.HttpConnection;

import parserr.Tag;
import Connction.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * A HTML Form Element provides ready access to the form fields/controls that are associated with it. It also allows a
 * form to easily be submitted.
 */
public class FormElement extends Element {
    private final Elements elements = new Elements();

    /**
     * Create a new, standalone form tags.
     *
     * @param tag        tag of this tags
     * @param baseUri    the base URI
     * @param attributes initial attributes
     */
    public FormElement(Tag tag, String baseUri, Attributes attributes) {
        super(tag, baseUri, attributes);
    }

    /**
     * Get the list of form control elements associated with this form.
     * @return form controls associated with this tags.
     */
    public Elements elements() {
        return elements;
    }

    /**
     * Add a form control tags to this form.
     * @param element form control to add
     * @return this form tags, for chaining
     */
    public FormElement addElement(Element element) {
        elements.add(element);
        return this;
    }

    @Override
    protected void removeChild(Node out) {
        super.removeChild(out);
        elements.remove(out);
    }

    /**
     * Prepare to submit this form. A Connection object is created with the request set up from the form values. You
     * can then set up other options (like user-agent, timeout, cookies), then execute it.
     * @return a connection prepared from the values of this form.
     * @throws IllegalArgumentException if the form's absolute action URL cannot be determined. Make sure you pass the
     * document's base URI when parsing.
     */
    public HttpConnection submit() {
        String action = hasAttr("action") ? absUrl("action") : baseUri();

        HttpConnection.Method method = attr("method").toUpperCase().equals("POST") ?
                HttpConnection.Method.POST : HttpConnection.Method.GET;

        return HttpConnection.connect(action)
                .data(formData())
                .method(method);
    }

    /**
     * Get the data that this form submits. The returned list is a copy of the data, and changes to the contents of the
     * list will not be reflected in the DOM.
     * @return a list of key vals
     */
    public List<HttpConnection.KeyVal> formData() {
        ArrayList<HttpConnection.KeyVal> data = new ArrayList<>();

        // iterate the form control elements and accumulate their values
        for (Element el: elements) {
            if (!el.tag().isFormSubmittable()) continue; // contents are form listable, superset of submitable
            if (el.hasAttr("disabled")) continue; // skip disabled form inputs
            String name = el.attr("name");
            if (name.length() == 0) continue;
            String type = el.attr("type");

            if ("select".equals(el.tagName())) {
                Elements options = el.select("option[selected]");
                boolean set = false;
                for (Element option: options) {
                    data.add(HttpConnection.KeyVal.create(name, option.val()));
                    set = true;
                }
                if (!set) {
                    Element option = el.select("option").first();
                    if (option != null)
                        data.add(HttpConnection.KeyVal.create(name, option.val()));
                }
            } else if ("checkbox".equalsIgnoreCase(type) || "radio".equalsIgnoreCase(type)) {
                // only add checkbox or radio if they have the checked attribute
                if (el.hasAttr("checked")) {
                    final String val = el.val().length() >  0 ? el.val() : "on";
                    data.add(HttpConnection.KeyVal.create(name, val));
                }
            } else {
                data.add(HttpConnection.KeyVal.create(name, el.val()));
            }
        }
        return data;
    }
}
