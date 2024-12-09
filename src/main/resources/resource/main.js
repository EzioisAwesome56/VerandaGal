function set_full() {
    // get our element
    var element = document.getElementById("selimage");
    // swap out the class applied to it
    element.removeAttribute("class");
    element.classList.add("fit-width")
}
function set_half() {
    // get our element
    var element = document.getElementById("selimage");
    // swap out the class applied to it
    element.removeAttribute("class");
    element.classList.add("fit-width-half")
}
function set_thirty() {
    // get our element
    var element = document.getElementById("selimage");
    // swap out the class applied to it
    element.removeAttribute("class");
    element.classList.add("fit-width-thirty")
}
function toggle_sidebar(){
    // get the sidebar element
    var element = document.getElementById("sidebar");
    // check to see what state it is in right now
    if (element.classList.contains("hide")){
        element.classList.remove("hide");
    } else {
        element.classList.add("hide");
    }
}