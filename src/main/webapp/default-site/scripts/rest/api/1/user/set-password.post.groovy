/*
 * Crafter Studio Web-content authoring solution
 * Copyright (C) 2007-2017 Crafter Software Corporation.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 */

import groovy.json.JsonSlurper
import scripts.api.SecurityServices

def result = [:]
def requestBody = request.reader.text

def slurper = new JsonSlurper()
def parsedReq = slurper.parseText(requestBody)

def username = parsedReq.username;
def token = parsedReq.token;
def newPassword = parsedReq.new;

def context = SecurityServices.createContext(applicationContext, request)
try {
    def success = SecurityServices.setUserPassword(context, username, token, newPassword)
    if (success) {
        def locationHeader = request.getRequestURL().toString().replace(request.getPathInfo().toString(), "") + "/api/1/services/api/1/user/get?username=" + username
        response.addHeader("Location", locationHeader)
        result.status = "OK"
        response.setStatus(200)
    } else {
        response.setStatus(404)
        result.status = "User not found"
        return result;
    }
} catch (Exception e) {
    response.setStatus(500)
    result.status = "Internal server error"
    return result;
}